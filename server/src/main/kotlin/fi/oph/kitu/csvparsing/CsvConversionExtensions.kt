package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import io.opentelemetry.api.trace.Span
import java.io.ByteArrayOutputStream

inline fun <reified T> Iterable<T>.writeAsCsv(
    outputStream: ByteArrayOutputStream,
    args: CsvArgs = CsvArgs(),
) {
    val csvMapper: CsvMapper = getCsvMapper<T>()
    val schema = getSchema<T>(csvMapper, args)

    csvMapper
        .writerFor(Iterable::class.java)
        .with(schema)
        .writeValue(outputStream, this)
}

/**
 * Converts retrieved String response into a list that is the type of Body.
 */
inline fun <reified T> String.asCsv(args: CsvArgs = CsvArgs()): List<T> {
    val span = Span.current()

    span.setAttribute("serialization.isEmptyList", this.isBlank())

    if (this.isBlank()) {
        return emptyList()
    }

    val csvMapper = getCsvMapper<T>()
    val schema = getSchema<T>(csvMapper, args)

    return try {
        csvMapper
            .readerFor(T::class.java)
            .with(schema)
            .readValues<T>(this)
            .readAll()
    } catch (e: InvalidFormatException) {
        span.apply {
            setAttribute("serialization.isInvalidFormatException", true)
            setAttribute("serialization.value", e.value.toString())
            setAttribute("serialization.targetType", e.targetType.toString())
            setAttribute("serialization.path", e.path.joinToString())
        }
        throw e
    }
}
