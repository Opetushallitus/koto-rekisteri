package fi.oph.kitu.csvparsing

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
import kotlin.reflect.full.findAnnotation

class CsvParser(
    val event: LoggingEventBuilder,
    val columnSeparator: Char = ',',
    val lineSeparator: String = "\n",
    val useHeader: Boolean = false,
    val quoteChar: Char = '"',
) {
    inline fun <reified T> getSchema(csvMapper: CsvMapper): CsvSchema {
        event.add(
            "serialization.schema.args.type" to T::class.java.name,
            "serialization.schema.args.columnSeparator" to columnSeparator.toString(),
            "serialization.schema.args.useHeader" to useHeader,
            "serialization.schema.args.quoteChar" to quoteChar,
        )

        val schema: CsvSchema =
            csvMapper
                .typedSchemaFor(T::class.java)
                .withColumnSeparator(columnSeparator)
                .withLineSeparator(lineSeparator)
                .withUseHeader(useHeader)
                .withQuoteChar(quoteChar)

        schema
            .sortedBy { column -> column.index }
            .forEach { column ->
                val paddedIndex = column.index.toString().padStart(2, '0')
                event.add(
                    "serialization.schema.column[$paddedIndex].index" to column.index,
                    "serialization.schema.column[$paddedIndex].name" to column.name,
                    "serialization.schema.column[$paddedIndex].type" to column.type,
                )
            }

        return schema
    }

    inline fun <reified T> CsvMapper.Builder.withFeatures(): CsvMapper.Builder {
        val mapperFeatures = T::class.findAnnotation<Features>()?.features
        if (mapperFeatures != null) {
            for (feature in mapperFeatures) {
                this.enable(feature)
            }
        }

        return this
    }

    fun CsvMapper.withModules(): CsvMapper {
        this.registerModule(JavaTimeModule())
        val oidSerializerModule = SimpleModule()
        oidSerializerModule.addSerializer(Oid::class.java, OidSerializer())
        this.registerModule(oidSerializerModule)

        return this
    }

    inline fun <reified T> getCsvMapper(): CsvMapper =
        CsvMapper
            .builder()
            .withFeatures<T>()
            .build()
            .withModules()

    inline fun <reified T> streamDataAsCsv(
        outputStream: ByteArrayOutputStream,
        data: Iterable<T>,
    ) {
        val csvMapper: CsvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)

        csvMapper
            .writerFor(Iterable::class.java)
            .with(schema)
            .writeValue(outputStream, data)
    }

    /**
     * Converts retrieved String response into a list that is the type of Body.
     */
    inline fun <reified T> convertCsvtToResults(csvString: String): Iterable<Result<T>> {
        if (csvString.isBlank()) {
            event.add("serialization.isEmptyList" to true)
            return emptyList()
        }

        event.add("serialization.isEmptyList" to false)

        val csvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)

        val iterator =
            csvMapper
                .readerFor(T::class.java)
                .with(schema)
                .readValues<T?>(csvString)

        // the lines are needed to read line by line in order to distinguish all erroneous lines
        val data = mutableListOf<Result<T>>()
        while (iterator.hasNext()) {
            data.add(runCatching { iterator.nextValue() })
        }

        return data
    }
}

fun <T> Iterable<Result<T>>.foldWithErrors(
    continueOnError: Boolean,
    action: (Iterable<CsvExportError>) -> Unit,
): List<T> {
    val errors = mutableListOf<CsvExportError>()
    val data = mutableListOf<T>()
    // Add errors to error list
    this.forEachIndexed { index, result ->
        result
            .onFailure { e ->
                when (e) {
                    is InvalidFormatException -> errors.add(InvalidFormatCsvExportError(index, e))
                    else -> errors.add(SimpleCsvExportError(index, e))
                }
            }.onSuccess { row -> data.add(row) }

        if (errors.isNotEmpty()) {
            action(errors)

            if (!continueOnError) {
                throw RuntimeException(
                    "Unable to convert string to csv, because the string had ${errors.count()} error(s).",
                )
            }
        }
    }

    return data
}
