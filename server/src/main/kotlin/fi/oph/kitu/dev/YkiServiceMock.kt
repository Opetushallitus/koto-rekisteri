package fi.oph.kitu.dev

import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class YkiServiceMock(
    private val suoritusRepository: YkiSuoritusRepository,
    private val suoritusErrorService: YkiSuoritusErrorService,
    private val suoritusMapper: YkiSuoritusMappingService,
    private val auditLogger: AuditLogger,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(
        csv: String,
        from: Instant,
        dryRun: Boolean?,
    ) = logger
        .atInfo()
        .withEventAndPerformanceCheck { event ->
            val parser = CsvParser(event)
            val (suoritukset, errors) = parser.safeConvertCsvToData<YkiSuoritusCsv>(csv ?: "")

            suoritusErrorService.handleErrors(event, errors)
            val nextSince = suoritusErrorService.findNextSearchRange(suoritukset, errors, from)

            event.add("yki.suoritukset.receivedCount" to suoritukset.size)

            if (dryRun != true) {
                val saved = suoritusRepository.saveAll(suoritusMapper.convertToEntityIterable(suoritukset))
                event.add("importedSuorituksetSize" to saved.count())
                auditLogger.logAll("YKI suoritus imported", saved) { suoritus ->
                    arrayOf(
                        "principal" to "yki.importSuoritukset",
                        "suoritus.id" to suoritus.suoritusId,
                    )
                }
            }
            return@withEventAndPerformanceCheck nextSince
        }
}
