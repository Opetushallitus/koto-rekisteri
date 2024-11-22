package fi.oph.kitu.csvparsing

import org.springframework.web.client.RestClientException
import java.io.OutputStream

inline fun <reified T> Iterable<T>.writeAsCsv(
    outputStream: OutputStream,
    args: CsvArgs = CsvArgs(),
) {
    val csvMapper = getCsvMapper<T>()
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
    } catch (e: Exception) {
        throw RestClientException("Could not parse string as CSV into a type ${T::class.java.name}.", e)
    }
}
