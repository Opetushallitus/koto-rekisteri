package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.setSerializationErrorToAttributes
import fi.oph.kitu.logging.setAttribute
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.stereotype.Service

@Service
class YkiArvioijaErrorService(
    private val mappingService: YkiArvioijaErrorMappingService,
    private val repository: YkiArvioijaErrorRepository,
    private val tracer: Tracer,
) {
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
}
