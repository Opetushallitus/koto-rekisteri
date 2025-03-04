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

    inline fun <reified T> getSchema(csvMapper: CsvMapper): CsvSchema {
        event.add("serialization.schema.args.type" to T::class.java.name)

        return csvMapper
            .typedSchemaFor(T::class.java)
            .withColumnSeparator(columnSeparator)
            .withLineSeparator(lineSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)
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

    inline fun <reified T> getCsvMapper() =
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
     * Tries to convert retrieved String response into a list that is the type of Body.
     */
    inline fun <reified T> tryConvertCsvToData(csvString: String): Result<List<T>> {
        if (csvString.isBlank()) {
            event.add("serialization.isEmptyList" to true)
            return Result.success(emptyList())
        }

        event.add("serialization.isEmptyList" to false)

        val csvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)

        // the lines are needed to read line by line in order to distinguish all erroneous lines
        val errors = mutableListOf<CsvExportError>()

        val iterator =
            csvMapper
                .readerFor(T::class.java)
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
            return Result.success(data)
        }

        // add all errors to log
        errors.forEachIndexed { i, error ->
            event.add("serialization.error[$i].index" to i)
            for (kvp in error.keyValues) {
                event.add("serialization.error[$i].${kvp.first}" to kvp.second)
            }
        }

        return Result.failure(
            RuntimeException("Unable to convert string to csv, because the string had ${errors.count()} error(s)."),
        )
    }

    /**
     * Converts retrieved String response into a list that is the type of Body.
     */
    inline fun <reified T> convertCsvToData(csvString: String): List<T> = tryConvertCsvToData<T>(csvString).getOrThrow()
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
