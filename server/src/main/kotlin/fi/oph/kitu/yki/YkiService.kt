package fi.oph.kitu.yki

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.findDifferentProperties
import fi.oph.kitu.ignoreEmptyValues
import fi.oph.kitu.ilmoittautumisjarjestelma.IlmoittautumisjarjestelmaService
import fi.oph.kitu.logging.AuditLogOperation
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.observability.use
import fi.oph.kitu.splitIntoValuesAndErrors
import fi.oph.kitu.yki.arvioijat.YkiArvioijaArviointioikeus
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusFilter
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusOrder
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeama
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeamaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class YkiService(
    @param:Qualifier("solkiRestClient")
    private val solkiRestClient: RestClient,
    private val suoritusRepository: YkiSuoritusRepository,
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val suoritusMapper: YkiSuoritusMappingService,
    private val arvioijaRepository: YkiArvioijaRepository,
    private val arvioijaMapper: YkiArvioijaMappingService,
    private val arvioijaErrorService: YkiArvioijaErrorService,
    private val ilmoittautumisjarjestelma: IlmoittautumisjarjestelmaService,
    private val suoritusPoikkeamaRepository: YkiSuoritusPoikkeamaRepository,
    private val auditLogger: AuditLogger,
    private val parser: CsvParser,
    private val tracer: Tracer,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun findSuoritusById(id: Int): YkiSuoritusEntity? =
        suoritusRepository.findById(id).also { suoritus ->
            suoritus?.suorittajanOID?.let { oid ->
                auditLogger.log(AuditLogOperation.YkiSuoritusViewed, oid)
            }
        }

    fun debugImportSuoritukset(from: Instant): String =
        tracer
            .spanBuilder("YkiService.debugImportSuoritukset")
            .startSpan()
            .use { span ->
                val url = "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}"
                val response =
                    solkiRestClient
                        .get()
                        .uri(url)
                        .retrieve()
                        .toEntity<String>()
                response.body ?: "No body"
            }

    fun checkYkiAnomalies(from: Instant): Instant =
        tracer
            .spanBuilder("YkiService.importSuoritukset")
            .startSpan()
            .use { span ->
                val startTime = Instant.now()
                val url = "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}"

                val response =
                    solkiRestClient
                        .get()
                        .uri(url)
                        .retrieve()
                        .toEntity<String>()

                val (suoritukset, errors) =
                    parser
                        .convertCsvToData<YkiSuoritusCsv>(response.body.orEmpty())
                        .splitIntoValuesAndErrors()

                val hasErrors = suoritusErrorService.handleErrors(errors)

                span.setAttribute("yki.suoritukset.receivedCount", suoritukset.size.toLong())

                val entities = suoritusMapper.convertToEntityIterable(suoritukset)

                suoritusPoikkeamaRepository.deleteAll()
                entities.forEach { entity ->
                    suoritusRepository
                        .findLatestBySolkiIds(listOf(entity.solkiId))
                        .firstOrNull()
                        ?.let { existing ->
                            val diff =
                                entity
                                    .findDifferentProperties(existing, "SOLKI")
                                    .ignoreEmptyValues()
                            val time = Instant.now()
                            diff.forEach { (key, value) ->
                                val poikkeama =
                                    YkiSuoritusPoikkeama(
                                        solkiId = entity.solkiId,
                                        kentta = key,
                                        arvoKitussa = value.first.toString(),
                                        arvoSolkissa = value.second.toString(),
                                        havaittu = time,
                                    )
                                suoritusPoikkeamaRepository.save(poikkeama)
                                logger.error(
                                    "Havaittu poikkeama yki-suorituksen tiedoissa verratuuna Solkin tietoihin: $poikkeama",
                                )
                            }
                        }
                }

                if (hasErrors) {
                    throw Error.CsvConversionError("importYkiSuoritukset", errors)
                }

                return@use startTime.minusSeconds(60 * 5)
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
        class EmptyArvioijatResponse : Error("Empty body on arvioijat response")

        class EmptyArvioijat : Error("Unexpected empty list of arvioijat")

        class CsvConversionError(
            service: String,
            errors: List<CsvExportError>,
        ) : Error("service '$service' received ${errors.size} errors from csv conversion.")
    }
}
