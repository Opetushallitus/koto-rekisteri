package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException

class InvalidFormatCsvExportError(
    lineNumber: Int,
    context: String?,
    exception: InvalidFormatException,
) : CsvExportError(lineNumber, context, exception) {
    private val fieldWithValidationError = exception.path.firstOrNull()?.fieldName ?: ""

    init {
        keyValues.putAll(
            listOf(
                "value" to exception.value,
                "path" to exception.path,
                "targetType" to exception.targetType,
                "fieldWithValidationError" to fieldWithValidationError,
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
