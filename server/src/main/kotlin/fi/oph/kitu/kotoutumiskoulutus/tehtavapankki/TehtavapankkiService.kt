package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import fi.oph.kitu.PeerService
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addHttpResponse
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import io.awspring.cloud.s3.S3Template
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class TehtavapankkiService(
    @Qualifier("restClientBuilderForLargeResponses")
    private val restClientBuilder: RestClient.Builder,
    private val s3Template: S3Template,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    @Value("\${kitu.kotoutumiskoulutus.tehtavapankki.bucket}")
    lateinit var bucket: String

    private val restClient by lazy { restClientBuilder.baseUrl(koealustaBaseUrl).build() }

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

    fun uploadTehtavapankki(response: TehtavapankkiResponse) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        response.questionbanks.forEachIndexed { index, (courseid, coursename, xml) ->
            val sanitizedCoursename = sanitizeFilename(coursename)
            val filename = "$courseid-$sanitizedCoursename/$now-$index.xml"
            val stream = xml.byteInputStream(Charsets.UTF_8)
            s3Template.upload(bucket, filename, stream)
        }
    }

    fun importTehtavapankki() =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                event.add(
                    "function" to "local_completion_export_export_question_bank",
                )

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

                event
                    .add("request.token" to koealustaToken)
                    .addHttpResponse(PeerService.Koealusta, uri = "/webservice/rest/server.php", response)

                uploadTehtavapankki(response.body!!)
            }.apply {
                addDefaults("koealusta.importTehtavapankki")
                addDatabaseLogs()
            }.getOrThrow()
}
