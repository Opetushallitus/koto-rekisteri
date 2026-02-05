package fi.oph.kitu.yki

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
import fi.oph.kitu.yki.arvioijat.YkiArvioijaArviointioikeus
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class YkiService(
    private val suoritusRepository: YkiSuoritusRepository,
    private val suoritusMapper: YkiSuoritusMappingService,
    private val arvioijaRepository: YkiArvioijaRepository,
    private val auditLogger: AuditLogger,
    private val parser: CsvParser,
    private val tracer: Tracer,
) {
    fun generateSuorituksetCsvStream(includeVersionHistory: Boolean): ByteArrayOutputStream =
        tracer
            .spanBuilder("YkiService.generateSuorituksetCsvStream")
            .startSpan()
            .use { span ->
                val newParser = parser.withUseHeader(true)
                val suoritukset = allSuoritukset(includeVersionHistory)
                span.setAttribute("dataCount", suoritukset.count())
                val writableData = suoritusMapper.convertToResponseIterable(suoritukset)
                val outputStream = ByteArrayOutputStream()
                newParser.streamDataAsCsv(outputStream, writableData)

                return@use outputStream
            }

    @WithSpan
    fun allSuoritukset(versionHistory: Boolean): List<YkiSuoritusEntity> =
        suoritusRepository
            .find(distinct = !versionHistory)
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
}
