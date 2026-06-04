package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import com.fasterxml.jackson.annotation.JsonProperty
import fi.oph.kitu.restclient.withJacksonStreamMaxStringLength
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.file.Files
import java.time.Instant

@Service
@Profile("!local-opintopolku")
class TehtavapankkiClientImpl(
    val restClientBuilder: RestClient.Builder,
) : TehtavapankkiClient {
    @Value($$"${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value($$"${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    private val restClient by lazy {
        restClientBuilder
            .baseUrl(koealustaBaseUrl)
            .withJacksonStreamMaxStringLength(200_000_000)
            .build()
    }

    @WithSpan
    override fun importQuestionBanks(): TehtavapankkiResponse {
        Span.current().setAttribute("function", "local_completion_export_export_question_bank")
        val raw =
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
                .toEntity<RawTehtavapankkiResponse>()
                .body!!
        return TehtavapankkiResponse(
            questionbanks =
                raw.questionbanks.map { qb ->
                    TehtavapankkiResponse.Questionbank(
                        courseId = qb.courseid,
                        courseName = qb.coursename,
                        published = qb.coursestartdate.toLong().let { Instant.ofEpochMilli(it) },
                        generated = qb.filegenerated.toLong().let { Instant.ofEpochMilli(it) },
                        version = qb.questionbankversion,
                        language = qb.language,
                        xml = buildXmlSource(qb.downloadurl),
                    )
                },
        )
    }

    private fun buildXmlSource(downloadUrl: String): XmlSource {
        val uri =
            UriComponentsBuilder
                .fromUriString(downloadUrl)
                .queryParam("token", koealustaToken)
                .build()
                .toUri()
        return FileXmlSource(spoolToTempFile(uri), deleteOnClose = true)
    }

    private fun spoolToTempFile(uri: URI) =
        Files.createTempFile("tehtavapankki-", ".xml").also { tmp ->
            restClient.get().uri(uri).exchange { _, response ->
                response.body.use { input ->
                    Files.newOutputStream(tmp).use { out -> input.copyTo(out) }
                }
            }
        }

    private data class RawTehtavapankkiResponse(
        @param:JsonProperty("questionbanks")
        val questionbanks: List<RawQuestionbank>,
    )

    private data class RawQuestionbank(
        @param:JsonProperty("courseid")
        val courseid: Int,
        @param:JsonProperty("coursename")
        val coursename: String,
        @param:JsonProperty("coursestartdate")
        val coursestartdate: Int,
        @param:JsonProperty("questionbankversion")
        val questionbankversion: String,
        @param:JsonProperty("language")
        val language: String,
        @param:JsonProperty("downloadurl")
        val downloadurl: String,
        @param:JsonProperty("filegenerated")
        val filegenerated: Int,
    )
}
