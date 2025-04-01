package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.PeerService
import fi.oph.kitu.SortDirection
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.setAttribute
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.Instant

@Service
class KoealustaService(
    val restClientBuilder: RestClient.Builder,
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    private val mappingService: KoealustaMappingService,
    private val auditLogger: AuditLogger,
    private val kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
    private val tracer: Tracer,
) {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    private val restClient by lazy { restClientBuilder.baseUrl(koealustaBaseUrl).build() }

    fun getSuoritukset(
        orderBy: KielitestiSuoritusColumn,
        orderByDirection: SortDirection,
    ): List<KielitestiSuoritus> =
        kielitestiSuoritusRepository
            .findAllSorted(orderBy.entityName, orderByDirection)
            .toList()
            .also {
                auditLogger.logAll("Kielitesti suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                    )
                }
            }

    fun importSuoritukset(from: Instant): Instant =
        tracer.spanBuilder("koealusta.import.suoritukset").startSpan().use { span ->
            val remoteFunction = "local_completion_export_get_completions"

            span.setAttribute("function", remoteFunction)
            span.setAttribute("from", from.toString())

            val response =
                restClient
                    .get()
                    .uri(
                        "/webservice/rest/server.php?wstoken={token}&wsfunction={function}&moodlewsrestformat=json&from={from}",
                        mapOf<String?, Any>(
                            "token" to koealustaToken,
                            "function" to remoteFunction,
                            "from" to from.epochSecond,
                        ),
                    ).accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity<String>()

            if (response.body == null) {
                return from
            }

            val (suoritukset, validationFailure) = mappingService.responseStringToEntity(response.body!!)

            validationFailure?.let {
                kielitestiSuoritusErrorRepository.saveAll(mappingService.convertErrors(it.validationErrors))
                kielitestiSuoritusErrorRepository.saveAll(mappingService.convertErrors(it.oppijanumeroExceptions))

                span.setAttribute("errors.db.saved", it.validationErrors.size + it.oppijanumeroExceptions.size)
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

            span.setAttribute("db.saved", savedSuoritukset.count())

            if (validationFailure != null &&
                (
                    validationFailure.validationErrors.isNotEmpty() ||
                        validationFailure.oppijanumeroExceptions.isNotEmpty()
                )
            ) {
                return from
            }

            return suoritukset.maxOfOrNull { it.timeCompleted } ?: from
        }
}
