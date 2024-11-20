package fi.oph.kitu.csvparsing

import org.springframework.web.client.RestClientException
import java.io.StringWriter

inline fun <reified T> Iterable<T>.toCsvString(
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): String {
    val csvMapper = getCsvMapper<T>()
    val schema =
        getSchema<T>(
            csvMapper,
            columnSeparator,
            useHeader,
            quoteChar,
        )

    val writer = StringWriter()

    csvMapper
        .writerFor(Iterable::class.java)
        .with(schema)
        .writeValue(writer, this)

    val str = writer.toString()
    return str
}

/**
 * Converts retrieved String response into a list that is the type of Body.
 */
inline fun <reified T> String.asCsv(
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): List<T> {
    if (this.isBlank()) {
        return emptyList()
    }

    val csvMapper = getCsvMapper<T>()
    val schema =
        getSchema<T>(
            csvMapper,
            columnSeparator,
            useHeader,
            quoteChar,
        )

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
