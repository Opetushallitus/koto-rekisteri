package fi.oph.kitu.yki

import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.setSerializationErrorToAttributes
import fi.oph.kitu.logging.setAttribute
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service

@Service
class SimpleErrorHandler(
    private val tracer: Tracer,
) {
    fun <Repository : CrudRepository<T, ID>, T, ID> handleErrors(
        repository: Repository,
        errors: List<CsvExportError>,
        data: List<T>,
    ): Boolean =
        tracer
            .spanBuilder("SimpleErrorHandler.handleErrors")
            .startSpan()
            .use { span ->
                span.setSerializationErrorToAttributes(errors)

                if (errors.isEmpty()) {
                    repository.deleteAll()
                    return false
                }

                repository.saveAll(data).also {
                    span.setAttribute("errors.addedSize", it.count())
                }
                return true
            }
}
