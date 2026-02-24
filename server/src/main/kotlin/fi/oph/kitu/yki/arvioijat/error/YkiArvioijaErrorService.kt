package fi.oph.kitu.yki.arvioijat.error

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
class YkiArvioijaErrorService(
    private val repository: YkiArvioijaErrorRepository,
    private val auditLogger: AuditLogger,
) {
    @WithSpan
    fun countErrors(): Long = repository.count()

    @WithSpan
    fun getErrors(
        orderBy: YkiArvioijaErrorColumn = YkiArvioijaErrorColumn.VirheenLuontiaika,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiArvioijaErrorEntity> =
        repository
            .findAllSorted(orderBy.entityName, orderByDirection)
            .toList()
            .also {
                auditLogger.logAllInternalOnly("Yki arvioija errors viewed", it) { error ->
                    arrayOf("arvioija.error.id" to error.id)
                }
            }
}
