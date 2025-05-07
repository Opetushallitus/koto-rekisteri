package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.setSerializationErrorToAttributes
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.setAttribute
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service

@Service
class YkiArvioijaErrorService(
    private val mappingService: YkiArvioijaErrorMappingService,
    private val repository: YkiArvioijaErrorRepository,
    private val tracer: Tracer,
    private val auditLogger: AuditLogger,
) {
    @WithSpan
    fun countErrors(): Long = repository.count()

    fun handleErrors(errors: List<CsvExportError>): Boolean =
        tracer
            .spanBuilder("YkiArvioijaErrorService.handleErrors")
            .startSpan()
            .use { span ->
                span.setSerializationErrorToAttributes(errors)

                // add actual errors to database
                if (errors.isEmpty()) {
                    repository.deleteAll()
                    return@use false
                }

                val entities = mappingService.convertToEntityIterable(errors)
                repository.saveAll(entities).also {
                    span.setAttribute("errors.addedSize", it.count())
                }

                return@use true
            }

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
