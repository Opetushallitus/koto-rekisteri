package fi.oph.kitu.csvparsing

import fi.oph.kitu.logging.setAttribute
import io.opentelemetry.api.trace.Span

fun Span.setSerializationErrorToAttributes(errors: List<CsvExportError>) {
    this.setAttribute("errors.size", errors.size)
    this.setAttribute("errors.truncate", errors.isEmpty())

    // trace serialization errors
    errors.forEachIndexed { i, error ->
        this.setAttribute("serialization.error[$i].index", i)
        for (kvp in error.keyValues) {
            this.setAttribute("serialization.error[$i].${kvp.key}", kvp.value.toString())
        }
    }
}
