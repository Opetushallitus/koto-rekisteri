package fi.oph.kitu.yki

import fi.oph.kitu.auditlogs.AuditLogOperation
import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.ilmoittautumisjarjestelma.IlmoittautumisjarjestelmaService
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.util.findDifferentProperties
import fi.oph.kitu.util.ignoreEmptyValues
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
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

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
    fun checkYkiAnomalies(from: Instant): Instant {
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

        Span.current().setAttribute("yki.suoritukset.receivedCount", suoritukset.size.toLong())

        val entities = suoritusMapper.convertToEntityIterable(suoritukset)

        entities.forEach { entity ->
            val existing =
                suoritusRepository
                    .findLatestBySolkiIds(listOf(entity.solkiId))
                    .firstOrNull()
            val time = Instant.now()
            val poikkeamat =
                if (existing == null) {
                    listOf(
                        YkiSuoritusPoikkeama(
                            solkiId = entity.solkiId,
                            kentta = YkiSuoritusPoikkeama.SUORITUS_PUUTTUU_KITUSTA,
                            arvoKitussa = "",
                            arvoSolkissa =
                                "${entity.sukunimi} ${entity.etunimet}, " +
                                    "${entity.tutkintotaso}, ${entity.tutkintopaiva}",
                            havaittu = time,
                            tutkintopaiva = entity.tutkintopaiva,
                            tutkintokieli = entity.tutkintokieli,
                            tutkintotaso = entity.tutkintotaso,
                        ),
                    )
                } else {
                    entity
                        .findDifferentProperties(existing, "SOLKICSV")
                        .ignoreEmptyValues()
                        .map { (key, value) ->
                            YkiSuoritusPoikkeama(
                                solkiId = entity.solkiId,
                                kentta = key,
                                arvoKitussa = value.second.toString(),
                                arvoSolkissa = value.first.toString(),
                                havaittu = time,
                                tutkintopaiva = existing.tutkintopaiva,
                                tutkintokieli = existing.tutkintokieli,
                                tutkintotaso = existing.tutkintotaso,
                            )
                        }
                }
            poikkeamat.forEach { poikkeama ->
                suoritusPoikkeamaRepository.save(poikkeama)
                logger.error(
                    "Havaittu poikkeama yki-suorituksen tiedoissa verratuuna Solkin tietoihin: $poikkeama",
                )
            }
        }

        if (hasErrors) {
            throw Error.CsvConversionError("importYkiSuoritukset", errors)
        }

        return startTime.minusSeconds(60 * 5)
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
        class CsvConversionError(
            service: String,
            errors: List<CsvExportError>,
        ) : Error("service '$service' received ${errors.size} errors from csv conversion.")
    }
}
