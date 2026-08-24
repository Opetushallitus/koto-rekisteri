package fi.oph.kitu.csvparsing

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.jdbc.findAllSorted
import org.springframework.data.repository.PagingAndSortingRepository

fun <E : Any, R : PagingAndSortingRepository<E, Long>> getCsvExportErrorsSorted(
    repository: R,
    auditLogger: AuditLogger,
    auditMessage: String,
    columnName: String,
    orderByDirection: SortDirection,
    auditProperties: (E) -> Array<Pair<String, Any?>>,
): List<E> =
    repository
        .findAllSorted(columnName, orderByDirection)
        .toList()
        .also { auditLogger.logAllInternalOnly(auditMessage, it, auditProperties) }
