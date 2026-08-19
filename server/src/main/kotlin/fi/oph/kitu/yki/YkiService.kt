package fi.oph.kitu.yki

import fi.oph.kitu.auditlogs.AuditLogOperation
import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.ilmoittautumisjarjestelma.IlmoittautumisjarjestelmaService
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.util.findDifferentProperties
import fi.oph.kitu.util.ignoreEmptyValues
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.util.result.splitIntoValuesAndErrors
import fi.oph.kitu.yki.arvioijat.YkiArvioijaArviointioikeus
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusFilter
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusOrder
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeama
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeamaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.Instant
import java.time.format.DateTimeFormatter

data class ExtendedFilter(
    val filter: YkiSuoritusFilter,
    val oppijanumeroUnavailable: Boolean,
)

@Service
class YkiService(
    @param:Qualifier("solkiRestClient")
    private val solkiRestClient: RestClient,
    private val suoritusRepository: YkiSuoritusRepository,
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val suoritusMapper: YkiSuoritusMappingService,
    private val arvioijaRepository: YkiArvioijaRepository,
    private val suoritusPoikkeamaRepository: YkiSuoritusPoikkeamaRepository,
    private val auditLogger: AuditLogger,
    private val parser: CsvParser,
    private val oppijanumeroService: OppijanumeroService,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @WithSpan
    fun extendFilterWithLinkedOids(filter: YkiSuoritusFilter): ExtendedFilter =
        filter.extendHenkiloOids(oppijanumeroService).fold(
            ifLeft = { error ->
                logger.warn("Oppijanumeropalvelu ei vastannut, YKI-haku tehdään vain annetuilla oideilla", error)
                ExtendedFilter(filter, oppijanumeroUnavailable = true)
            },
            ifRight = { extended -> ExtendedFilter(extended, oppijanumeroUnavailable = false) },
        )

    @WithSpan
    fun extendFilterWithLinkedOidsOrThrow(filter: YkiSuoritusFilter): YkiSuoritusFilter =
        filter.extendHenkiloOids(oppijanumeroService).getOrThrow()

    fun findSuoritusById(id: Int): YkiSuoritusEntity? =
        suoritusRepository.findById(id).also { suoritus ->
            suoritus?.suorittajanOID?.let { oid ->
                auditLogger.log(AuditLogOperation.YkiSuoritusViewed, oid)
            }
        }

    @WithSpan
    fun debugImportSuoritukset(from: Instant): String {
        val url = "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}"
        val response =
            solkiRestClient
                .get()
                .uri(url)
                .retrieve()
                .toEntity<String>()
        return response.body ?: "No body"
    }

    @WithSpan
    fun allSuoritukset(
        versionHistory: Boolean,
        filter: YkiSuoritusFilter = YkiSuoritusFilter(),
    ): List<YkiSuoritusEntity> =
        suoritusRepository
            .find(distinct = !versionHistory, filter = filter)
            .toList()
            .also {
                auditLogger.logAllInternalOnly("Yki suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                    )
                }
            }

    @WithSpan
    fun allSuorituksetIncludingOpiskeluoikeusOid(
        versionHistory: Boolean,
        filter: YkiSuoritusFilter = YkiSuoritusFilter(),
    ): List<YkiSuoritusEntity> {
        val suoritukset = allSuoritukset(versionHistory, filter)
        val opiskeluoikeusOidit =
            suoritusRepository.findOpiskeluoikeusOidsBySolkiIds(
                suoritukset.filter { it.koskiOpiskeluoikeus == null }.map { it.solkiId },
            )
        return suoritukset.map { suoritus ->
            if (suoritus.koskiOpiskeluoikeus == null) {
                suoritus.copy(koskiOpiskeluoikeus = opiskeluoikeusOidit[suoritus.solkiId])
            } else {
                suoritus
            }
        }
    }

    @WithSpan
    fun countSuoritukset(
        filter: YkiSuoritusFilter = YkiSuoritusFilter(),
        versionHistory: Boolean = false,
    ): Long = suoritusRepository.countSuoritukset(filter = filter, distinct = !versionHistory)

    @WithSpan
    fun findSuorituksetPaged(
        filter: YkiSuoritusFilter = YkiSuoritusFilter(),
        order: YkiSuoritusOrder = YkiSuoritusOrder(),
        versionHistory: Boolean = false,
        limit: Int,
        offset: Int,
    ): List<YkiSuoritusEntity> =
        suoritusRepository
            .findForListView(
                filter = filter,
                order = order,
                distinct = !versionHistory,
                limit = limit,
                offset = offset,
            ).toList()
            .also {
                auditLogger.logAllInternalOnly("Yki suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                    )
                }
            }

    @WithSpan
    fun allArvioijat(
        orderBy: YkiArvioijaColumn = YkiArvioijaColumn.Sukunimi,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiArvioijaArviointioikeus> =
        arvioijaRepository
            .allArviontioikeudet(orderBy, orderByDirection)
            .toList()
            .also {
                auditLogger.logAllInternalOnly("Yki arvioija viewed", it) { arvioija ->
                    arrayOf("arvioija.oppijanumero" to arvioija.arvioijanOppijanumero)
                }
            }

    sealed class Error(
        message: String,
    ) : Throwable(message) {
        class CsvConversionError(
            service: String,
            errors: List<CsvExportError>,
        ) : Error("service '$service' received ${errors.size} errors from csv conversion.")
    }
}
