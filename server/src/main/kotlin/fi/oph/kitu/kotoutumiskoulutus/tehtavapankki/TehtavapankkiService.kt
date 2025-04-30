package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import fi.oph.kitu.logging.use
import fi.oph.kitu.withJacksonStreamMaxStringLength
import io.awspring.cloud.s3.S3Template
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Profile("!ci & !e2e & !test")
class TehtavapankkiService(
    private val restClientBuilder: RestClient.Builder,
    private val s3Template: S3Template,
    private val tracer: Tracer,
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

    fun dryRun(): Boolean = (bucket?.trim()?.length ?: 0) == 0

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

    fun uploadTehtavapankki(response: TehtavapankkiResponse) =
        tracer
            .spanBuilder("TehtavapankkiService.uploadTehtavapankki")
            .startSpan()
            .use { span ->
                span.setAttribute("dryRun", dryRun())

                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                response.questionbanks.forEachIndexed { index, (courseid, coursename, xml) ->
                    val sanitizedCoursename = sanitizeFilename(coursename)
                    val filename = "$courseid-$sanitizedCoursename/$now-$index.xml"
                    val stream = xml.byteInputStream(Charsets.UTF_8)

                    if (!dryRun()) {
                        s3Template.upload(bucket!!, filename, stream)
                    }
                }
            }

    fun importTehtavapankki() =
        tracer
            .spanBuilder("TehtavapankkiService.importTehtavapankki")
            .startSpan()
            .use { span ->
                span.setAttribute("function", "local_completion_export_export_question_bank")

                val response =
                    restClient
                        .get()
                        .uri(
                            "/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction={function}",
                            mapOf<String?, Any>(
                                "token" to koealustaToken,
                                "function" to "local_completion_export_export_question_bank",
                            ),
                        ).accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity<TehtavapankkiResponse>()

                uploadTehtavapankki(response.body!!)
            }
}
