package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.yki.SimpleErrorHandler
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service

@Service
class YkiArvioijaErrorService(
    private val mappingService: YkiArvioijaErrorMappingService,
    private val repository: YkiArvioijaErrorRepository,
    private val auditLogger: AuditLogger,
    private val errorHandler: SimpleErrorHandler,
) {
    @WithSpan
    fun countErrors(): Long = repository.count()

    @WithSpan
    fun handleErrors(errors: List<CsvExportError>): Boolean =
        errorHandler.handleErrors(
            repository,
            errors,
            mappingService.convertToEntityIterable(errors),
        )

    @WithSpan
    fun getErrors(
        orderBy: YkiArvioijaErrorColumn = YkiArvioijaErrorColumn.VirheenLuontiaika,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiArvioijaErrorEntity> =
        repository
            .findAllSorted(orderBy.entityName, orderByDirection)
            .toList()
            .also {
                auditLogger.logAll("Yki arvioija errors viewed", it) { error ->
                    arrayOf("arvioija.error.id" to error.id)
                }
            }
}
