package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.restclient.withJacksonStreamMaxStringLength
import fi.oph.kitu.util.result.TypedResult
import io.awspring.cloud.s3.S3Template
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import software.amazon.awssdk.services.s3.S3Client
import java.io.ByteArrayInputStream
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

// Wired vain kun spring.cloud.aws.s3.enabled on tosi. Bean luodaan
// myös testeissä ja e2e:ssä kun ne osoittavat LocalStackiin; ajastettu
// import gateataan erikseen TehtavapankkiScheduledTasksissa.
@Service
@ConditionalOnProperty(
    name = ["spring.cloud.aws.s3.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class TehtavapankkiService(
    val restClientBuilder: RestClient.Builder,
    private val s3Template: S3Template,
    private val s3Client: S3Client,
    private val tracer: Tracer,
    private val parser: TehtavapankkiXmlParser,
) {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    @Value("\${kitu.kotoutumiskoulutus.tehtavapankki.bucket:#{null}}")
    var bucket: String? = null

    private val restClient by lazy {
        restClientBuilder
            .baseUrl(koealustaBaseUrl)
            .withJacksonStreamMaxStringLength(200_000_000)
            .build()
    }

    /**
     * 1. Replaces white spaces with underscore.
     * 2. Replaces any character that isn't a number, letter or underscore with nothing.
     * 3. Takes first 128 characters from the name-string
     */
    fun sanitizeFilename(string: String) =
        string
            .replace(' ', '_')
            .replace(Regex("\\W+"), "")
            .take(128)

    @WithSpan
    fun uploadTehtavapankki(response: TehtavapankkiResponse) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        response.questionbanks.forEachIndexed { index, (courseid, coursename, xml) ->
            val sanitizedCoursename = sanitizeFilename(coursename)
            val filename = "$courseid-$sanitizedCoursename/$now-$index.xml"
            val stream = xml.byteInputStream(Charsets.UTF_8)

            useS3 { bucketName ->
                upload(bucketName, filename, stream)
            }
        }
    }

    @WithSpan
    fun importTehtavapankki() {
        Span.current().setAttribute("function", "local_completion_export_export_question_bank")

        val response =
            restClient
                .get()
                .uri(
                    "/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction={function}",
                    mapOf<String, Any>(
                        "token" to koealustaToken,
                        "function" to "local_completion_export_export_question_bank",
                    ),
                ).accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity<TehtavapankkiResponse>()

        uploadTehtavapankki(response.body!!)
    }

    @WithSpan
    fun listTehtavapaketit(): Map<String, List<TehtavapakettiObject>> =
        listAllTehtavapaketit()
            .groupBy { it.filename.substringBefore("/") }
            .mapValues { (_, v) -> v.sortedByDescending { it.timestamp } }

    @WithSpan
    fun listAllTehtavapaketit(): List<TehtavapakettiObject> =
        useS3 { bucket ->
            listAllObjects(bucket).map {
                TehtavapakettiObject(
                    key = it.location.`object`,
                    filename = it.filename,
                    size = it.contentLength(),
                    timestamp = Instant.ofEpochMilli(it.lastModified()),
                )
            }
        }.orEmpty()

    @WithSpan
    fun getTemporaryDownloadUrl(key: String): URL? =
        useS3 { bucket ->
            if (objectExists(bucket, key)) {
                createSignedGetURL(bucket, key, java.time.Duration.ofMinutes(10))
            } else {
                null
            }
        }

    /**
     * Poistaa kustakin "kansiosta" (avaimen `/`-prefiksi) duplikaatti-objektit:
     * objektit, joilla on sama koko ja ETag (eli sama sisältö). Vanhin pidetään,
     * loput poistetaan. Eri kansioiden välillä ei verrata.
     */
    @WithSpan
    fun removeDuplicates(): CleanupResult? =
        useS3 { bucket ->
            val objects =
                s3Client
                    .listObjectsV2Paginator { it.bucket(bucket) }
                    .contents()
                    .toList()

            val toDelete =
                objects
                    .filter { it.key().contains("/") }
                    .groupBy { it.key().substringBefore("/") }
                    .flatMap { (_, folderObjects) ->
                        folderObjects
                            .groupBy { it.size() to it.eTag().normalizeETag() }
                            .filter { (_, dupes) -> dupes.size > 1 }
                            .flatMap { (_, dupes) ->
                                dupes.sortedBy { it.lastModified() }.drop(1)
                            }
                    }

            toDelete.forEach { obj ->
                s3Client.deleteObject { it.bucket(bucket).key(obj.key()) }
            }

            Span.current().setAttribute("scanned", objects.size.toLong())
            Span.current().setAttribute("deleted", toDelete.size.toLong())

            CleanupResult(
                scanned = objects.size,
                deleted = toDelete.map { it.key() },
            )
        }

    /**
     * Lukee XML:n S3:sta, parsii sen ja kirjoittaa sisällä olevat upotetut
     * <file>-blobit erillisinä S3-objekteina. Avain muodostetaan XML:n sijainnista:
     * `{tehtäväpankki-kansio}/{xml-tiedoston basename} assets/{tiedoston nimi}`.
     * Esim. `42-Suomi/2026-01-01.xml` → `42-Suomi/2026-01-01 assets/audio.mp3`.
     *
     * Olemassaolevat assetit ylikirjoitetaan, jotta XML ja sen assetit pysyvät
     * synkassa. Yksittäisen tiedoston dekoodausvirhe ei keskeytä koko ajoa
     * vaan kirjataan tulokseen `failed`-listaan.
     */
    @WithSpan
    fun extractAndUploadAssets(xmlKey: String): TypedResult<AssetExtractResult, TehtavapankkiParseError> {
        Span.current().setAttribute("xml.key", xmlKey)
        val parsed = fetchAndParseFromS3(xmlKey)
        val quiz =
            when (parsed) {
                is TypedResult.Success -> parsed.value
                is TypedResult.Failure -> return TypedResult.Failure(parsed.error)
            }

        val prefix = "${xmlKey.removeSuffix(".xml")} assets/"
        val uploaded = mutableListOf<String>()
        val failed = mutableListOf<FailedAsset>()
        val decoder = Base64.getMimeDecoder()

        quiz.allEmbeddedFiles().forEach { file ->
            if (file.name.isBlank()) return@forEach
            val key = "$prefix${file.name}"
            val bytes =
                try {
                    decoder.decode(file.content)
                } catch (e: IllegalArgumentException) {
                    failed += FailedAsset(file.name, e.message ?: "base64 decode failed")
                    return@forEach
                }
            useS3 { bucket -> upload(bucket, key, ByteArrayInputStream(bytes)) }
            uploaded += key
        }

        Span.current().setAttribute("assets.uploaded", uploaded.size.toLong())
        Span.current().setAttribute("assets.failed", failed.size.toLong())

        return TypedResult.Success(AssetExtractResult(xmlKey, uploaded, failed))
    }

    @WithSpan
    fun fetchXmlBytes(key: String): TypedResult<ByteArray, TehtavapankkiParseError> {
        Span.current().setAttribute("s3.key", key)
        val resource =
            useS3 { bucket ->
                if (objectExists(bucket, key)) download(bucket, key) else null
            } ?: return TypedResult.Failure(TehtavapankkiParseError.NotFound)
        return try {
            TypedResult.Success(resource.inputStream.use { it.readBytes() })
        } catch (e: Throwable) {
            TypedResult.Failure(TehtavapankkiParseError.IO(e))
        }
    }

    @WithSpan
    fun fetchAndParseFromS3(key: String): TypedResult<TehtavapankkiQuiz, TehtavapankkiParseError> =
        when (val bytes = fetchXmlBytes(key)) {
            is TypedResult.Success -> parser.parse(bytes.value.inputStream())
            is TypedResult.Failure -> TypedResult.Failure(bytes.error)
        }

    @WithSpan
    private fun <T> useS3(f: S3Template.(bucketName: String) -> T): T? {
        val bucketName = bucket?.trim()
        Span.current().setAttribute("dryRun", bucketName.isNullOrBlank())
        return bucketName?.let {
            f(s3Template, bucketName)
        }
    }
}

data class TehtavapakettiObject(
    val key: String,
    val filename: String,
    val size: Long,
    val timestamp: Instant,
)

data class CleanupResult(
    val scanned: Int,
    val deleted: List<String>,
)

data class AssetExtractResult(
    val xmlKey: String,
    val uploadedAssets: List<String>,
    val failed: List<FailedAsset>,
)

data class FailedAsset(
    val filename: String,
    val reason: String,
)

// AWS palauttaa ETagin lainausmerkeissä; karsitaan ne ennen vertailua.
private fun String.normalizeETag(): String = this.trim('"')

private fun TehtavapankkiQuiz.allEmbeddedFiles(): List<EmbeddedFile> =
    questions.flatMap { question ->
        when (question) {
            is DescriptionQuestion -> question.questiontext?.embeddedFiles.orEmpty()
            is MultichoiceQuestion -> question.questiontext?.embeddedFiles.orEmpty()
            is ShortanswerQuestion -> question.questiontext?.embeddedFiles.orEmpty()
            is EssayQuestion -> question.questiontext?.embeddedFiles.orEmpty()
            is CloudpoodllQuestion -> question.questiontext?.embeddedFiles.orEmpty()
            is CategoryQuestion, is UnknownQuestion -> emptyList()
        }
    }
