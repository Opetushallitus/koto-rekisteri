package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.withEvent
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

inline fun <reified T> Iterable<T>.writeAsCsv(
    outputStream: ByteArrayOutputStream,
    args: CsvArgs = CsvArgs(),
) = LoggerFactory.getLogger(javaClass).atInfo().withEvent("csvparsing.writeAsCsv") { _ ->
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
    if (this.isBlank()) {
        args.event.add("serialization.isEmptyList" to true)
        return emptyList()
    }

    args.event.add("serialization.isEmptyList" to false)

    val csvMapper = getCsvMapper<T>()
    val schema = getSchema<T>(csvMapper, args)

    return try {
        csvMapper
            .readerFor(T::class.java)
            .with(schema)
            .readValues<T>(this)
            .readAll()
    } catch (e: InvalidFormatException) {
        args.event.add(
            "serialization.isInvalidFormatException" to true,
            "serialization.value" to e.value,
            "serialization.targetType" to e.targetType,
            "serialization.path" to e.path,
        )

        throw e
    }
}
