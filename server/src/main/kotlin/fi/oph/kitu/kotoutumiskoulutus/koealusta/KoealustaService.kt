package fi.oph.kitu.kotoutumiskoulutus.koealusta

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.auditlogs.PeerService
import fi.oph.kitu.jdbc.replaceAll
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.CustomKielitestiSuoritusRepository
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritusRepository
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.error.KielitestiSuoritusErrorRepository
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.restclient.withLenientStringConverter
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
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
    private val customKielitestiSuoritusRepository: CustomKielitestiSuoritusRepository,
    private val mappingService: KoealustaMappingService,
    private val auditLogger: AuditLogger,
    private val kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
) {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    private val restClient by lazy {
        restClientBuilder
            .baseUrl(koealustaBaseUrl)
            .withLenientStringConverter()
            .build()
    }

    @WithSpan("koealusta.import.suoritukset")
    fun importSuoritukset(from: Instant): Instant {
        val span = Span.current()
        val remoteFunction = "local_completion_export_get_completions"

        span.setAttribute("function", remoteFunction)
        span.setAttribute("from", from.toString())

        val response =
            restClient
                .get()
                .uri(
                    "/webservice/rest/server.php?wstoken={token}&wsfunction={function}&moodlewsrestformat=json&from={from}",
                    mapOf<String, Any>(
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

        val validationErrors = mappingService.convertErrors(validationFailure?.validationErrors.orEmpty())
        val oppijanumeroErrors = mappingService.convertErrors(validationFailure?.oppijanumeroExceptions.orEmpty())
        kielitestiSuoritusErrorRepository.replaceAll(validationErrors + oppijanumeroErrors)

        val savedSuoritukset =
            suoritukset
                .mapNotNull {
                    if (!customKielitestiSuoritusRepository.exists(it)) {
                        kielitestiSuoritusRepository.save(it)
                    } else {
                        null
                    }
                }.also {
                    auditLogger.logAllInternalOnly("Kielitesti suoritus imported", it) { suoritus ->
                        arrayOf(
                            "suoritus.id" to suoritus.id,
                            "principal.name" to "koealusta.import",
                            "peer.service" to PeerService.Koealusta.value,
                        )
                    }
                }

        span.setAttribute("db.saved", savedSuoritukset.count())

        if (validationFailure != null && validationFailure.isNotEmpty()) {
            span.setAttribute("db.saved.error.validation", validationErrors.count())
            span.setAttribute("db.saved.error.onr", oppijanumeroErrors.count())
            return from
        }

        return suoritukset.maxOfOrNull { it.suoritusaika } ?: from
    }
}
