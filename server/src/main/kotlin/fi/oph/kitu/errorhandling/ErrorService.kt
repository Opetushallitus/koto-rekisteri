package fi.oph.kitu.errorhandling

import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.setSerializationErrorToAttributes
import fi.oph.kitu.jdbc.replaceAll
import fi.oph.kitu.observability.setAttribute
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.stereotype.Service

@Service
class ErrorService(
    private val tracer: Tracer
) {
    fun handleErrors(errors: List<CsvExportError>): Boolean =
        tracer
            .spanBuilder("ErrorService.handleErrors")
            .startSpan()
            .use { span ->
                span.setSerializationErrorToAttributes(errors)


                repository
                    .replaceAll(mappingService.convertToEntityIterable(errors))
                    .also { span.setAttribute("errors.addedSize", it.count()) }
                    .let { it.count() > 0 }
}