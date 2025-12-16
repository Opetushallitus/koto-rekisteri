package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.PeerService
import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.jdbc.replaceAll
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.sortedWithDirectionBy
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.io.ByteArrayOutputStream
import java.time.Instant

@Service
class KoealustaService(
    val restClientBuilder: RestClient.Builder,
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    private val mappingService: KoealustaMappingService,
    private val auditLogger: AuditLogger,
    private val kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
    private val csvParser: CsvParser,
    private val organisaatioService: OrganisaatioService,
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
            .sortByName(orderBy, orderByDirection)
            .also {
                auditLogger.logAllInternalOnly("Kielitesti suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                        "suoritus.oppijanumero" to suoritus.oppijanumero,
                    )
                }
            }

    @WithSpan
    fun getErrors(
        sortColumn: KielitestiSuoritusErrorColumn,
        sortDirection: SortDirection,
    ) = kielitestiSuoritusErrorRepository
        .findAllSorted(sortColumn.entityName, sortDirection)
        .also {
            auditLogger.logAllInternalOnly("Kielitesti suoritus error viewed", it) { error ->
                arrayOf("suoritus.error.id" to error.id)
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
                return@use from
            }

            val (suoritukset, validationFailure) = mappingService.responseStringToEntity(response.body!!)

            val validationErrors = mappingService.convertErrors(validationFailure?.validationErrors.orEmpty())
            val oppijanumeroErrors = mappingService.convertErrors(validationFailure?.oppijanumeroExceptions.orEmpty())
            kielitestiSuoritusErrorRepository.replaceAll(validationErrors + oppijanumeroErrors)

            val savedSuoritukset =
                suoritukset
                    .mapNotNull {
                        try {
                            kielitestiSuoritusRepository.save(it)
                        } catch (error: DbActionExecutionException) {
                            if (error.cause is DuplicateKeyException) null else throw error
                        }
                    }.also {
                        auditLogger.logAllInternalOnly("Kielitesti suoritus imported", it) { suoritus ->
                            arrayOf(
                                "suoritus.id" to suoritus.id,
                                "principal" to "koealusta.import",
                                "peer.service" to PeerService.Koealusta.value,
                            )
                        }
                    }

            span.setAttribute("db.saved", savedSuoritukset.count())

            if (validationFailure != null && validationFailure.isNotEmpty()) {
                span.setAttribute("db.saved.error.validation", validationErrors.count())
                span.setAttribute("db.saved.error.onr", oppijanumeroErrors.count())
                return@use from
            }

            return@use suoritukset.maxOfOrNull { it.timeCompleted } ?: from
        }

    fun generateSuorituksetCsvStream(
        orderBy: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        orderByDirection: SortDirection = SortDirection.DESC,
    ): ByteArrayOutputStream =
        tracer
            .spanBuilder("KoealustaService.generateSuorituksetCsvStream")
            .startSpan()
            .use { span ->
                val suoritukset = getSuoritukset(orderBy, orderByDirection)
                span.setAttribute("dataCount", suoritukset.count())

                val outputStream = ByteArrayOutputStream()
                csvParser
                    .withUseHeader(true)
                    .streamDataAsCsv(outputStream, suoritukset.map { KielitestiSuoritusCsv.of(it) })

                return@use outputStream
            }

    fun List<KielitestiSuoritus>.sortByName(
        orderBy: KielitestiSuoritusColumn,
        orderByDirection: SortDirection,
    ): List<KielitestiSuoritus> =
        when (orderBy) {
            KielitestiSuoritusColumn.Organisaatio -> {
                val nimet = organisaatioService.getOrganisaatiot().nimet
                this.sortedWithDirectionBy(orderByDirection) {
                    it.schoolOid
                        ?.let { oid -> nimet[oid]?.toString() }
                        ?: it.schoolOid?.toString().orEmpty()
                }
            }

            else -> {
                this
            }
        }
}
