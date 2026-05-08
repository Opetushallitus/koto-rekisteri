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
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    @WithSpan
    fun fetchAndParseFromS3(key: String): TypedResult<TehtavapankkiQuiz, TehtavapankkiParseError> {
        Span.current().setAttribute("s3.key", key)
        val resource =
            useS3 { bucket ->
                if (objectExists(bucket, key)) download(bucket, key) else null
            } ?: return TypedResult.Failure(TehtavapankkiParseError.NotFound)
        val stream =
            try {
                resource.inputStream
            } catch (e: Throwable) {
                return TypedResult.Failure(TehtavapankkiParseError.IO(e))
            }
        return stream.use { parser.parse(it) }
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
