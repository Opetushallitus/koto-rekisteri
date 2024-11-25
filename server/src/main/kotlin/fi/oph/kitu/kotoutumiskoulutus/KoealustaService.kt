package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.PeerService
import fi.oph.kitu.logging.Logging
import fi.oph.kitu.logging.add
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.Instant

@Service
class KoealustaService(
    private val restClientBuilder: RestClient.Builder,
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    private val jacksonObjectMapper: ObjectMapper,
    private val mappingService: KoealustaMappingService,
) {
    private val auditLogger = Logging.auditLogger()

    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    private val restClient by lazy { restClientBuilder.baseUrl(koealustaBaseUrl).build() }

    private inline fun <reified T> tryParseMoodleResponse(json: String): T {
        try {
            return jacksonObjectMapper.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION).readValue<T>(json)
        } catch (e: Throwable) {
            throw tryParseMoodleError(json, e)
        }
    }

    private fun tryParseMoodleError(
        json: String,
        originalException: Throwable,
    ): MoodleException {
        try {
            return MoodleException(jacksonObjectMapper.readValue<MoodleErrorMessage>(json))
        } catch (e: Throwable) {
            throw RuntimeException(
                "Could not parse Moodle error message: ${e.message} while handling parsing error",
                originalException,
            )
        }
    }

    @WithSpan
    fun importSuoritukset(from: Instant): Instant {
        val response =
            restClient
                .get()
                .uri(
                    "/webservice/rest/server.php?wstoken={token}&wsfunction={function}&moodlewsrestformat=json&from={from}",
                    mapOf<String?, Any>(
                        "token" to koealustaToken,
                        "function" to "local_completion_export_get_completions",
                        "from" to from.epochSecond,
                    ),
                ).accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity<String>()

        if (response.body == null) {
            return from
        }

        val suorituksetResponse =
            tryParseMoodleResponse<KoealustaSuorituksetResponse>(response.body!!)

        val suoritukset =
            mappingService.convertToEntity(suorituksetResponse)

        val savedSuoritukset = kielitestiSuoritusRepository.saveAll(suoritukset)

        for (suoritus in savedSuoritukset) {
            auditLogger
                .atInfo()
                .add(
                    "principal" to "koealusta.import",
                    "peer.service" to PeerService.Koealusta.value,
                    "suoritus.id" to suoritus.id,
                ).log("Kielitesti suoritus imported")
        }

        return suoritukset.maxOfOrNull { it.timeCompleted } ?: from
    }
}
