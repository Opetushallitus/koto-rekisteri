package fi.oph.kitu.csvparsing

import fi.oph.kitu.logging.add
import org.slf4j.spi.LoggingEventBuilder

interface CsvParserExtension {
    fun onInit(parser: CsvParser)

    fun <T> beforeFunctionCall(
        parser: CsvParser,
        input: T,
        type: Class<*>,
    )

    fun onErrorFunctionCall(
        parser: CsvParser,
        exception: CsvExportException,
    )

    fun afterFunctionCall(parser: CsvParser)
}

/** Extension for CsvParser to log events. */
class CsvParserExtensionEvent(
    val event: LoggingEventBuilder,
) : CsvParserExtension {
    override fun onInit(parser: CsvParser) {
        event.add(
            "serialization.schema.args.columnSeparator" to parser.columnSeparator.toString(),
            "serialization.schema.args.lineSeparator" to parser.lineSeparator,
            "serialization.schema.args.useHeader" to parser.useHeader,
            "serialization.schema.args.quoteChar" to parser.quoteChar,
        )
    }

    private fun logStringInput(string: String) = event.add("serialization.isEmptyList" to string.isBlank())

    override fun <T> beforeFunctionCall(
        parser: CsvParser,
        input: T,
        type: Class<*>,
    ) {
        event.add("serialization.schema.args.type" to type.name)
        when (input) {
            is String -> logStringInput(input)
        }
    }

    override fun onErrorFunctionCall(
        parser: CsvParser,
        exception: CsvExportException,
    ) {
        // add all errors to log
        exception.errors.forEachIndexed { i, error ->
            event.add("serialization.error[$i].index" to i)
            for (kvp in error.keyValues) {
                event.add("serialization.error[$i].${kvp.first}" to kvp.second)
            }
        }
    }

    override fun afterFunctionCall(parser: CsvParser) {
        TODO("Not yet implemented")
    }
}
