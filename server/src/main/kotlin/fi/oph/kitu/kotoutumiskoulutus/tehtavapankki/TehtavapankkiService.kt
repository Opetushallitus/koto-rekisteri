package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import fi.oph.kitu.PeerService
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addHttpResponse
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Service
class TehtavapankkiService(
    @Qualifier("restClientBuilderForLargeResponses")
    private val restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    private val restClient by lazy { restClientBuilder.baseUrl(koealustaBaseUrl).build() }

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
            }.apply {
                addDefaults("koealusta.importTehtavapankki")
                addDatabaseLogs()
            }.getOrThrow()
}
