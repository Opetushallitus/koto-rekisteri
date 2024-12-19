package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import fi.oph.kitu.logging.add
import org.slf4j.spi.LoggingEventBuilder

class InvalidFormatCsvExportError(
    lineNumber: Int,
    exception: InvalidFormatException,
) : CsvExportError(lineNumber, exception) {
    init {
        keyValues.add(Pair("value", exception.value))
        keyValues.add(Pair("path", exception.path))
        keyValues.add(Pair("targetType", exception.targetType))
    }
}

class SimpleCsvExportError(
    lineNumber: Int,
    exception: Throwable,
) : CsvExportError(lineNumber, exception)

abstract class CsvExportError(
    lineNumber: Int,
    exception: Throwable,
) {
    val keyValues = mutableListOf<Pair<String, Any>>()

    init {
        keyValues.add(Pair("lineNumber", lineNumber))
        keyValues.add(Pair("exception", exception))
    }
}

fun LoggingEventBuilder.addErrors(errors: Iterable<CsvExportError>) {
    // add all errors to log
    errors.forEachIndexed { i, error ->
        this.add("serialization.error[$i].index" to i)
        for (kvp in error.keyValues) {
            this.add("serialization.error[$i].${kvp.first}" to kvp.second)
        }
    }
}
