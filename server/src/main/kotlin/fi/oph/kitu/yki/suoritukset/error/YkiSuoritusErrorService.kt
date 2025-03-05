package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.csvparsing.CsvExportError
import org.springframework.stereotype.Service

@Service
class YkiSuoritusErrorService(
    private val mappingService: YkiSuoritusErrorMappingService,
    private val repository: YkiSuoritusErrorRepository,
) {
    /**
     * Save errors. Returns list of errors that were saved.
     */
    fun saveErrors(errors: List<CsvExportError>): Iterable<YkiSuoritusErrorEntity> {
        val entities = mappingService.convertToEntityIterable(errors)
        val savedEntities = repository.saveAll(entities)
        return savedEntities
    }
}
