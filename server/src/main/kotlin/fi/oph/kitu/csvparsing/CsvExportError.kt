package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException

class InvalidFormatCsvExportError(
    lineNumber: Int,
    context: String?,
    exception: InvalidFormatException,
) : CsvExportError(lineNumber, context, exception) {
    init {
        keyValues.addAll(
            listOf(
                "value" to exception.value,
                "path" to exception.path,
                "targetType" to exception.targetType,
            ),
        )
    }
}

class SimpleCsvExportError(
    lineNumber: Int,
    context: String?,
    exception: Throwable,
) : CsvExportError(lineNumber, context, exception)

abstract class CsvExportError(
    lineNumber: Int,
    val context: String?,
    exception: Throwable,
) {
    val keyValues =
        mutableListOf<Pair<String, Any>>(
            "lineNumber" to lineNumber,
            "exception" to exception,
        )
}
