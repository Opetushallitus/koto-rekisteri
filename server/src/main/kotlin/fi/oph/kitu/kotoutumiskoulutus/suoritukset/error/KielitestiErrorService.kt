package fi.oph.kitu.kotoutumiskoulutus.suoritukset.error

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.jdbc.findAllSorted
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class KielitestiErrorService(
    private val kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
    private val auditLogger: AuditLogger,
    private val tracer: Tracer,
    private val csvParser: CsvParser,
) {
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

    fun generateErrorsCsvStream(
        orderBy: KielitestiSuoritusErrorColumn = KielitestiSuoritusErrorColumn.VirheenLuontiaika,
        orderByDirection: SortDirection = SortDirection.DESC,
    ): ByteArrayOutputStream =
        tracer
            .spanBuilder("KoealustaService.generateErrorsCsvStream")
            .startSpan()
            .use { span ->
                val errors = getErrors(orderBy, orderByDirection)
                span.setAttribute("dataCount", errors.count())
                val outputStream = ByteArrayOutputStream()
                csvParser
                    .withUseHeader(true)
                    .streamDataAsCsv(outputStream, errors)

                return@use outputStream
            }
}
