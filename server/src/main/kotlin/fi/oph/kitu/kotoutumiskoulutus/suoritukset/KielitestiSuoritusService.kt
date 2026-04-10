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
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service

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
        customKielitestiSuoritusRepository.findById(id).also { suoritus ->
            suoritus?.oppijanumero?.let { oid ->
                auditLogger.log(AuditLogOperation.KielitestiSuoritusViewed, oid)
            }
        }

    fun getSuorituksetForCsv(
        orderBy: KielitestiSuoritusColumn = KielitestiSuoritusColumn.Suoritusaika,
        orderByDirection: SortDirection = SortDirection.DESC,
    ): List<KielitestiSuoritus> =
        tracer
            .spanBuilder("KoealustaService.getSuorituksetForCsv")
            .startSpan()
            .use { span ->
                val suoritukset = getSuoritukset(orderBy, orderByDirection)
                span.setAttribute("dataCount", suoritukset.count())

                val organisaatiot = organisaatioService.getOrganisaatiot()

                return suoritukset.map { it.copy(oppilaitos = organisaatiot.nimet[it.oppilaitosOid]?.fi) }
            }

    fun List<KielitestiSuoritus>.sortByName(
        orderBy: KielitestiSuoritusColumn,
        orderByDirection: SortDirection,
    ): List<KielitestiSuoritus> =
        when (orderBy) {
            KielitestiSuoritusColumn.Oppilaitos -> {
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
