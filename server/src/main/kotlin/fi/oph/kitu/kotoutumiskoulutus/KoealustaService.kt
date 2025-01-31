package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.PeerService
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addHttpResponse
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import fi.oph.kitu.oppijanumero.addValidationExceptions
import org.slf4j.LoggerFactory
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
    private val mappingService: KoealustaMappingService,
    private val auditLogger: AuditLogger,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    private val restClient by lazy { restClientBuilder.baseUrl(koealustaBaseUrl).build() }

    fun getSuoritukset() =
        kielitestiSuoritusRepository.findAll().toList().also {
            auditLogger.logAll("Kielitesti suoritus viewed", it) { suoritus ->
                arrayOf(
                    "suoritus.id" to suoritus.id,
                )
            }
        }

    fun importSuoritukset(from: Instant) =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                event.add("from" to from)

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

                event
                    .add("request.token" to koealustaToken)
                    .addHttpResponse(PeerService.Koealusta, uri = "/webservice/rest/server.php", response)

                if (response.body == null) {
                    return@withEventAndPerformanceCheck from
                }

                val suoritukset =
                    try {
                        mappingService.responseStringToEntity(response.body!!)
                    } catch (ex: KoealustaMappingService.Error.ValidationFailure) {
                        event.addValidationExceptions(ex.oppijanumeroExceptions, ex.validationErrors)
                        throw ex
                    }

                val savedSuoritukset =
                    kielitestiSuoritusRepository
                        .saveAll(suoritukset)
                        .also {
                            auditLogger.logAll("Kielitesti suoritus imported", it) { suoritus ->
                                arrayOf(
                                    "suoritus.id" to suoritus.id,
                                    "principal" to "koealusta.import",
                                    "peer.service" to PeerService.Koealusta.value,
                                )
                            }
                        }

                event.add("db.saved" to savedSuoritukset.count())

                return@withEventAndPerformanceCheck suoritukset.maxOfOrNull { it.timeCompleted } ?: from
            }.apply {
                addDefaults("koealusta.importSuoritukset")
                addDatabaseLogs()
            }.getOrThrow()
}
