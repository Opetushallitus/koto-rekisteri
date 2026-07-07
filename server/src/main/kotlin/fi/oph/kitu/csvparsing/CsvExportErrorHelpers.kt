package fi.oph.kitu.csvparsing

import fi.oph.kitu.auditlogs.AuditLogger
import fi.oph.kitu.jdbc.SortDirection
import fi.oph.kitu.jdbc.findAllSorted
import fi.oph.kitu.jdbc.replaceAll
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

fun <E : Any, R : CrudRepository<E, Long>> handleCsvExportErrors(
    repository: R,
    tracer: Tracer,
    spanName: String,
    errors: List<CsvExportError>,
    toEntities: (List<CsvExportError>) -> Iterable<E>,
): Boolean =
    tracer
        .spanBuilder(spanName)
        .startSpan()
        .use { span ->
            span.setSerializationErrorToAttributes(errors)
            repository
                .replaceAll(toEntities(errors))
                .also { span.setAttribute("errors.addedSize", it.count()) }
                .let { it.count() > 0 }
        }

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
