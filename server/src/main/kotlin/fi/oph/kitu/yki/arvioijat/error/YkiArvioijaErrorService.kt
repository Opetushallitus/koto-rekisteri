package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.csvparsing.getCsvExportErrorsSorted
import fi.oph.kitu.jdbc.SortDirection
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
        getCsvExportErrorsSorted(
            repository,
            auditLogger,
            "Yki arvioija errors viewed",
            orderBy.entityName,
            orderByDirection,
        ) {
            arrayOf("arvioija.error.id" to it.id)
        }
}
