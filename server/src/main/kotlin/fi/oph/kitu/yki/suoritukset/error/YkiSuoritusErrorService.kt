package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.add
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class YkiSuoritusErrorService(
    private val mappingService: YkiSuoritusErrorMappingService,
    private val repository: YkiSuoritusErrorRepository,
    private val auditLogger: AuditLogger,
) {
    fun countErrors(): Long = repository.count()

    fun handleErrors(
        event: LoggingEventBuilder,
        errors: List<CsvExportError>,
    ) {
        event.add(
            "errors.size" to errors.size,
            "errors.truncate" to errors.isEmpty(),
        )

        if (errors.isEmpty()) {
            repository.deleteAll()
        } else {
            val entities = mappingService.convertToEntityIterable(errors)
            repository.saveAll(entities).also {
                event.add("errors.addedSize" to it.count())
            }
        }
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
        orderBy: YkiSuoritusErrorColumn = YkiSuoritusErrorColumn.Created,
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
