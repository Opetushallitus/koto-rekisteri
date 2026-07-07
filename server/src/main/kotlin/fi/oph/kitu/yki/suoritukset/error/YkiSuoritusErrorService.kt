package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.getCsvExportErrorsSorted
import fi.oph.kitu.csvparsing.handleCsvExportErrors
import fi.oph.kitu.jdbc.SortDirection
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service

@Service
class YkiSuoritusErrorService(
    private val mappingService: YkiSuoritusErrorMappingService,
    private val repository: YkiSuoritusErrorRepository,
    private val auditLogger: AuditLogger,
    private val tracer: Tracer,
) {
    @WithSpan
    fun countErrors(): Long = repository.count()

    fun handleErrors(errors: List<CsvExportError>): Boolean =
        handleCsvExportErrors(repository, tracer, "YkiSuoritusErrorService.handleErrors", errors) {
            mappingService.convertToEntityIterable(it)
        }

    @WithSpan
    fun getErrors(
        orderBy: YkiSuoritusErrorColumn = YkiSuoritusErrorColumn.VirheenLuontiaika,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiSuoritusErrorEntity> =
        getCsvExportErrorsSorted(
            repository,
            auditLogger,
            "Yki suoritus errors viewed",
            orderBy.entityName,
            orderByDirection,
        ) {
            arrayOf("suoritus.error.id" to it.id)
        }
}
