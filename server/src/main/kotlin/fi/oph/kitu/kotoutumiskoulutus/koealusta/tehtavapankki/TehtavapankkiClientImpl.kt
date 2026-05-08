package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.restclient.withJacksonStreamMaxStringLength
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Service
@Profile("!local-opintopolku")
class TehtavapankkiClientImpl(
    val restClientBuilder: RestClient.Builder,
) : TehtavapankkiClient {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
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
        return restClient
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
            .body!!
    }
}
