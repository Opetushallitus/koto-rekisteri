package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.csvparsing.getCsvExportErrorsSorted
import fi.oph.kitu.jdbc.SortDirection
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
