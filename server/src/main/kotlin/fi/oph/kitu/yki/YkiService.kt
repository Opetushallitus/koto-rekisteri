package fi.oph.kitu.yki

import fi.oph.kitu.PeerService
import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addHttpResponse
import fi.oph.kitu.logging.use
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import fi.oph.kitu.splitIntoValuesAndErrors
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
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
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.use

@Service
class YkiService(
    @Qualifier("solkiRestClient")
    private val solkiRestClient: RestClient,
    private val suoritusRepository: YkiSuoritusRepository,
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val suoritusMapper: YkiSuoritusMappingService,
    private val arvioijaRepository: YkiArvioijaRepository,
    private val arvioijaMapper: YkiArvioijaMappingService,
    private val auditLogger: AuditLogger,
    private val parser: CsvParser,
    private val tracer: Tracer,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(from: Instant): Instant =
        tracer
            .spanBuilder("YkiService.importSuoritukset")
            .startSpan()
            .use { span ->

                val url = "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}"

                val response =
                    solkiRestClient
                        .get()
                        .uri(url)
                        .retrieve()
                        .toEntity<String>()

                val (suoritukset, errors) =
                    parser
                        .convertCsvToData<YkiSuoritusCsv>(response.body ?: "")
                        .splitIntoValuesAndErrors()

                val hasErrors = suoritusErrorService.handleErrors(errors)
                val nextSince = suoritusErrorService.findNextSearchRange(suoritukset, errors, from)

                span.setAttribute("yki.suoritukset.receivedCount", suoritukset.size.toLong())

                val saved = suoritusRepository.saveAll(suoritusMapper.convertToEntityIterable(suoritukset))
                span.setAttribute("importedSuorituksetSize", saved.count().toLong())
                auditLogger.logAll("YKI suoritus imported", saved) { suoritus ->
                    arrayOf(
                        "principal" to "yki.importSuoritukset",
                        "suoritus.id" to suoritus.suoritusId,
                    )
                }

                if (hasErrors) {
                    throw Error.CsvConversionError("importYkiSuoritukset", errors)
                }

                return@use nextSince
            }

    fun importYkiArvioijat() =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                val response =
                    solkiRestClient
                        .get()
                        .uri("arvioijat")
                        .retrieve()
                        .toEntity<String>()

                event.addHttpResponse(PeerService.Solki, "arvioijat", response)

                val (arvioijat) =
                    parser
                        .convertCsvToData<SolkiArvioijaResponse>(
                            response.body ?: throw Error.EmptyArvioijatResponse(),
                        ).splitIntoValuesAndErrors()

                event.add("yki.arvioijat.receivedCount" to arvioijat.size)

                if (arvioijat.isEmpty()) {
                    throw Error.EmptyArvioijat()
                }

                val importedArvioijat =
                    arvioijaRepository.saveAll(
                        arvioijaMapper.convertToEntityIterable(arvioijat),
                    )
                event.add("yki.arvioijat.importedCount" to importedArvioijat.count())

                auditLogger.logAll("YKI arvioija imported", importedArvioijat) { arvioija ->
                    arrayOf(
                        "principal" to "yki.importArvioijat",
                        "peer.service" to PeerService.Solki.value,
                        "arvioija.oppijanumero" to arvioija.arvioijanOppijanumero,
                    )
                }
            }.apply {
                addDefaults("yki.importArvioijat")
                addDatabaseLogs()
            }.getOrThrow()

    fun generateSuorituksetCsvStream(includeVersionHistory: Boolean): ByteArrayOutputStream =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                val newParser = parser.withUseHeader(true)
                val suoritukset = allSuoritukset(includeVersionHistory)
                event.add("dataCount" to suoritukset.count())
                val writableData = suoritusMapper.convertToResponseIterable(suoritukset)
                val outputStream = ByteArrayOutputStream()
                newParser.streamDataAsCsv(outputStream, writableData)

                return@withEventAndPerformanceCheck outputStream
            }.apply {
                addDefaults("yki.getSuorituksetCsv")
                addDatabaseLogs()
            }.getOrThrow()

    @WithSpan
    fun allSuoritukset(versionHistory: Boolean): List<YkiSuoritusEntity> =
        suoritusRepository
            .find(distinct = !versionHistory)
            .toList()
            .also {
                auditLogger.logAll("Yki suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                    )
                }
            }

    @WithSpan
    fun countSuoritukset(
        searchBy: String = "",
        versionHistory: Boolean = false,
    ): Long = suoritusRepository.countSuoritukset(searchBy = searchBy, distinct = !versionHistory)

    @WithSpan
    fun findSuorituksetPaged(
        searchStr: String = "",
        column: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
        direction: SortDirection,
        versionHistory: Boolean = false,
        limit: Int,
        offset: Int,
    ): List<YkiSuoritusEntity> =
        suoritusRepository
            .find(
                searchBy = searchStr,
                column = column,
                direction = direction,
                distinct = !versionHistory,
                limit = limit,
                offset = offset,
            ).toList()
            .also {
                auditLogger.logAll("Yki suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                    )
                }
            }

    @WithSpan
    fun allArvioijat(
        orderBy: YkiArvioijaColumn = YkiArvioijaColumn.Rekisteriintuontiaika,
        orderByDirection: SortDirection = SortDirection.DESC,
    ): List<YkiArvioijaEntity> =
        arvioijaRepository
            .findAllSorted(orderBy.entityName, orderByDirection)
            .toList()
            .also {
                auditLogger.logAll("Yki arvioija viewed", it) { arvioija ->
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
