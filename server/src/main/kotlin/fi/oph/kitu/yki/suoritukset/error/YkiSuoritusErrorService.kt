package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.setSerializationErrorToAttributes
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.jdbc.findAllSorted
import fi.oph.kitu.jdbc.replaceAll
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
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
        tracer
            .spanBuilder("YkiSuoritusErrorService.handleErrors")
            .startSpan()
            .use { span ->
                span.setSerializationErrorToAttributes(errors)
                repository
                    .replaceAll(mappingService.convertToEntityIterable(errors))
                    .also { span.setAttribute("errors.addedSize", it.count()) }
                    .let { it.count() > 0 }
            }

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
