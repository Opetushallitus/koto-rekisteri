package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException

class InvalidFormatCsvExportError(
    lineNumber: Int,
    context: String?,
    exception: InvalidFormatException,
) : CsvExportError(
        lineNumber,
        context,
        exception,
    ) {
    val fieldWithValidationError: String? = exception.path.firstOrNull()?.fieldName
    val valueWithValidationError: String = exception.value.toString()

    init {
        keyValues.putAll(
            listOf(
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
    val exception: Throwable,
) {
    val keyValues =
        mutableMapOf<String, Any>(
            "lineNumber" to lineNumber,
            "exception" to exception,
        )
}
