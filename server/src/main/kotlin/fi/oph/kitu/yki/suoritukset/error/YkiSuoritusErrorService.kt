package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.setSerializationErrorToAttributes
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.jdbc.replaceAll
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service

@Service
class YkiSuoritusErrorService(
    private val repository: YkiSuoritusErrorRepository,
    private val auditLogger: AuditLogger,
) {
    @WithSpan
    fun countErrors(): Long = repository.count()

    @WithSpan
    fun getErrors(
        orderBy: YkiSuoritusErrorColumn = YkiSuoritusErrorColumn.VirheenLuontiaika,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiSuoritusErrorEntity> =
        repository
            .findAllSorted(orderBy.entityName, orderByDirection)
            .toList()
            .also {
                auditLogger.logAllInternalOnly("Yki suoritus errors viewed", it) { error ->
                    arrayOf("suoritus.error.id" to error.id)
                }
            }
}
