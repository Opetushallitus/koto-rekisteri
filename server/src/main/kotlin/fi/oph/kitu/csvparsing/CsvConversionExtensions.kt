package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.JsonMappingException.Reference
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import fi.oph.kitu.logging.add
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException

inline fun <reified T> Iterable<T>.writeAsCsv(
    outputStream: ByteArrayOutputStream,
    args: CsvArgs, // Common args for all csv parsing
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
inline fun <reified T> String.asCsv(args: CsvArgs): List<T> {
    if (this.isBlank()) {
        args.event.add("serialization.isEmptyList" to true)
        return emptyList()
    }

    args.event.add("serialization.isEmptyList" to false)

    val csvMapper = getCsvMapper<T>()
    val schema = getSchema<T>(csvMapper, args)

    // the lines are needed to read line by line in order to distinguish all erroneus lines
    val errors = mutableListOf<CsvExportError>()
    val data =
        this
            .split(args.lineSeparator)
            .mapIndexed { index, line ->
                try {
                    csvMapper
                        .readerFor(T::class.java)
                        .with(schema)
                        .readValue<T?>(line)
                } catch (e: InvalidFormatException) {
                    errors.add(CsvExportError(index, e))
                    null
                }
            }.filterNotNull()

    if (errors.isEmpty()) {
        return data
    }

    // add all errors to log
    errors.forEachIndexed { index, error ->
        args.event.add(
            "serialization.error[$index].index" to index,
            "serialization.error[$index].lineNumber" to error.lineNumber,
            "serialization.error[$index].value" to error.value,
            "serialization.error[$index].path" to error.path,
            "serialization.error[$index].targetType" to error.targetType,
            "serialization.error[$index].exception," to error.exception,
        )
    }

    throw RuntimeException("Unable to convert string to csv, because the string had ${errors.count()} error(s).")
}

class CsvExportError(
    val lineNumber: Int,
    val exception: InvalidFormatException,
) {
    val value: Any = exception.value
    val path: List<Reference> = exception.path
    val targetType: Class<*> = exception.targetType
}
