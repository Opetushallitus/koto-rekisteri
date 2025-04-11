package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.setAttribute
import fi.oph.kitu.logging.use
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import io.opentelemetry.api.trace.Tracer
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class YkiSuoritusErrorService(
    private val mappingService: YkiSuoritusErrorMappingService,
    private val repository: YkiSuoritusErrorRepository,
    private val auditLogger: AuditLogger,
    private val tracer: Tracer,
) {
    fun countErrors(): Long = repository.count()

    fun handleErrors(errors: List<CsvExportError>): Boolean =
        tracer
            .spanBuilder("yki.suoritukset.errors")
            .startSpan()
            .use { span ->
                span.setAttribute("errors.size", errors.size)
                span.setAttribute("errors.truncate", errors.isEmpty())

                // trace serialization errors
                errors.forEachIndexed { i, error ->
                    span.setAttribute("serialization.error[$i].index", i)
                    for (kvp in error.keyValues) {
                        span.setAttribute("serialization.error[$i].${kvp.key}", kvp.value.toString())
                    }
                }

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

    fun findNextSearchRange(
        suoritukset: List<YkiSuoritusCsv>,
        errors: List<CsvExportError>,
        from: Instant,
    ): Instant =
        if (errors.isEmpty()) {
            suoritukset.maxOfOrNull { it.lastModified } ?: from
        } else {
            from
        }

    fun getErrors(
        orderBy: YkiSuoritusErrorColumn = YkiSuoritusErrorColumn.VirheenLuontiaika,
        orderByDirection: SortDirection = SortDirection.ASC,
    ): List<YkiSuoritusErrorEntity> =
        repository
            .findAllSorted(orderBy.entityName, orderByDirection)
            .toList()
            .also {
                auditLogger.logAll("Yki suoritus errors viewed", it) { error ->
                    arrayOf("suoritus.error.id" to error.id)
                }
            }
}
