package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import fi.oph.kitu.logging.add
import org.slf4j.spi.LoggingEventBuilder
import kotlin.reflect.KClass

class CsvParserWithEvent(
    private val event: LoggingEventBuilder,
    columnSeparator: Char,
    lineSeparator: String,
    useHeader: Boolean,
    quoteChar: Char,
) : CsvParser(columnSeparator, lineSeparator, useHeader, quoteChar) {
    init {
        event.add(
            "serialization.schema.args.columnSeparator" to columnSeparator.toString(),
            "serialization.schema.args.lineSeparator" to lineSeparator,
            "serialization.schema.args.useHeader" to useHeader,
            "serialization.schema.args.quoteChar" to quoteChar,
        )
    }

    override fun getSchema(
        csvMapper: CsvMapper,
        type: KClass<*>,
    ): CsvSchema {
        event.add("serialization.schema.args.type" to type::class.java.name)
        return super.getSchema(csvMapper, type)
    }

    override fun <T : Any> convertCsvToData(
        csvString: String,
        type: KClass<T>,
    ): List<T> {
        event.add("serialization.isEmptyList" to csvString.isBlank())
        return try {
            super.convertCsvToData(csvString, type)
        } catch (ex: CsvExportException) {
            // add all errors to log
            ex.errors.forEachIndexed { i, error ->
                event.add("serialization.error[$i].index" to i)
                for (kvp in error.keyValues) {
                    event.add("serialization.error[$i].${kvp.first}" to kvp.second)
                }
            }

            throw ex
        }
    }
}

fun CsvParser.withEvent(event: LoggingEventBuilder) =
    CsvParserWithEvent(
        event,
        columnSeparator,
        lineSeparator,
        useHeader,
        quoteChar,
    )
