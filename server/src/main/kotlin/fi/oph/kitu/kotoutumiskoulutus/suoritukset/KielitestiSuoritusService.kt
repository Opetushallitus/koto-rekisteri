package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.logging.AuditLogOperation
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.sortedWithDirectionBy
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import kotlin.jvm.optionals.getOrNull

@Service
class KielitestiSuoritusService(
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    private val customKielitestiSuoritusRepository: CustomKielitestiSuoritusRepository,
    private val auditLogger: AuditLogger,
    private val csvParser: CsvParser,
    private val organisaatioService: OrganisaatioService,
    private val tracer: Tracer,
) {
    @WithSpan
    fun getSuoritukset(
        orderBy: KielitestiSuoritusColumn,
        orderByDirection: SortDirection,
        search: String? = null,
    ): List<KielitestiSuoritus> =
        customKielitestiSuoritusRepository
            .findSuoritukset(search, orderBy, orderByDirection)
            .toList()
            .also {
                auditLogger.logAllInternalOnly("Kielitesti suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                        "suoritus.oppijanumero" to suoritus.oppijanumero,
                    )
                }
            }

    @WithSpan
    fun getSuoritusById(id: Int): KielitestiSuoritus? =
        kielitestiSuoritusRepository.findById(id).getOrNull().also { suoritus ->
            suoritus?.oppijanumero?.let { oid ->
                auditLogger.log(AuditLogOperation.KielitestiSuoritusViewed, oid)
            }
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

                val organisaatiot = organisaatioService.getOrganisaatiot()

                val outputStream = ByteArrayOutputStream()
                csvParser
                    .withUseHeader(true)
                    .streamDataAsCsv(outputStream, suoritukset.map { KielitestiSuoritusCsv.of(it, organisaatiot) })

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
                    it.oppilaitosOid
                        ?.let { oid -> nimet[oid]?.toString() }
                        ?: it.oppilaitosOid?.toString().orEmpty()
                }
            }

            else -> {
                this
            }
        }
}
