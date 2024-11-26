package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import fi.oph.kitu.logging.add
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClientException
import java.io.ByteArrayOutputStream

inline fun <reified T> Iterable<T>.writeAsCsv(
    outputStream: ByteArrayOutputStream,
    args: CsvArgs = CsvArgs(),
) {
    val event = LoggerFactory.getLogger(javaClass).atInfo()
    val start = System.currentTimeMillis()

    val csvMapper: CsvMapper = getCsvMapper<T>()
    val schema = getSchema<T>(csvMapper, args)

    try {
        csvMapper
            .writerFor(Iterable::class.java)
            .with(schema)
            .writeValue(outputStream, this)

        event.add("success" to true)
    } catch (ex: Exception) {
        event.setCause(ex)
        event.add("success" to false)
    } finally {
        event.add("elapsed" to System.currentTimeMillis() - start)
        event.log()
    }
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
