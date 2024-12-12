package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException

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
