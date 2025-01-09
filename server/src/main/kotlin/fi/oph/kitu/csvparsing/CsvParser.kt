package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.oph.kitu.logging.add
import org.ietf.jgss.Oid
import org.slf4j.spi.LoggingEventBuilder
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class CsvParser(
    val event: LoggingEventBuilder,
    val columnSeparator: Char = ',',
    val lineSeparator: String = "\n",
    val useHeader: Boolean = false,
    val quoteChar: Char = '"',
) {
    init {
        event.add(
            "serialization.schema.args.columnSeparator" to columnSeparator.toString(),
            "serialization.schema.args.lineSeparator" to lineSeparator,
            "serialization.schema.args.useHeader" to useHeader,
            "serialization.schema.args.quoteChar" to quoteChar,
        )
    }

    private fun getSchema(
        csvMapper: CsvMapper,
        type: KClass<*>,
    ): CsvSchema {
        event.add("serialization.schema.args.type" to type::class.java.name)

        return csvMapper
            .typedSchemaFor(type.java)
            .withColumnSeparator(columnSeparator)
            .withLineSeparator(lineSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)
    }

    private fun CsvMapper.Builder.withFeatures(type: KClass<*>): CsvMapper.Builder {
        val mapperFeatures = type.findAnnotation<Features>()?.features
        if (mapperFeatures != null) {
            for (feature in mapperFeatures) {
                this.enable(feature)
            }
        }

        return this
    }

    private fun CsvMapper.withModules(): CsvMapper {
        this.registerModules(
            JavaTimeModule(),
            SimpleModule().addSerializer(Oid::class.java, OidSerializer()),
        )

        return this
    }

    private fun getCsvMapper(type: KClass<*>): CsvMapper =
        CsvMapper
            .builder()
            .withFeatures(type)
            .build()
            .withModules()

    fun <T : Any> streamDataAsCsv(
        outputStream: ByteArrayOutputStream,
        data: Iterable<T>,
        type: KClass<T>,
    ) {
        val csvMapper: CsvMapper = getCsvMapper(type)
        val schema = getSchema(csvMapper, type)

        csvMapper
            .writerFor(Iterable::class.java)
            .with(schema)
            .writeValue(outputStream, data)
    }

    /**
     * Converts retrieved String response into a list that is the type of Body.
     */
    fun <T : Any> convertCsvToData(
        csvString: String,
        type: KClass<T>,
    ): List<T> {
        if (csvString.isBlank()) {
            event.add("serialization.isEmptyList" to true)
            return emptyList()
        }

        event.add("serialization.isEmptyList" to false)

        val csvMapper = getCsvMapper(type)
        val schema = getSchema(csvMapper, type)

        // the lines are needed to read line by line in order to distinguish all erroneous lines
        val errors = mutableListOf<CsvExportError>()

        val iterator =
            csvMapper
                .readerFor(type.java)
                .with(schema)
                .readValues<T?>(csvString)

        val data =
            iterator.toDataWithErrorHandling { index, e ->
                when (e) {
                    is InvalidFormatException -> errors.add(InvalidFormatCsvExportError(index, e))
                    else -> errors.add(SimpleCsvExportError(index, e))
                }
            }

        if (errors.isEmpty()) {
            return data
        }

        // add all errors to log
        errors.forEachIndexed { i, error ->
            event.add("serialization.error[$i].index" to i)
            for (kvp in error.keyValues) {
                event.add("serialization.error[$i].${kvp.first}" to kvp.second)
            }
        }

        throw RuntimeException("Unable to convert string to csv, because the string had ${errors.count()} error(s).")
    }
}

fun <T> MappingIterator<T>.toDataWithErrorHandling(
    onFailure: (index: Int, exception: Throwable) -> Unit = { _, _ -> },
): List<T> {
    val data = mutableListOf<T>()
    var index = 0
    while (this.hasNext()) {
        runCatching { data.add(this.nextValue()) }
            .onFailure { e -> onFailure(index, e) }
            .also { index++ }
    }

    return data
}
