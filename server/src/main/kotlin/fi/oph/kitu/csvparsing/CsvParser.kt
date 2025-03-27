package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.oph.kitu.Oid
import fi.oph.kitu.logging.add
import org.slf4j.spi.LoggingEventBuilder
import java.io.ByteArrayOutputStream
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
        oidSerializerModule.addDeserializer(Oid::class.java, OidDeserializer())
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
     * Converts retrieved String response into a list that is the type of Body.
     * Returns a pair:
     *  - first: the data
     *  - second: errors
     */
    inline fun <reified T> safeConvertCsvToData(csvString: String): Pair<List<T>, List<CsvExportError>> {
        if (csvString.isBlank()) {
            event.add("serialization.isEmptyList" to true)
            return Pair(emptyList(), emptyList())
        }

        event.add("serialization.isEmptyList" to false)

        val csvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)
        val lineSeparator =
            onlyOrNull(schema.lineSeparator)
                ?: throw IllegalStateException(
                    "Can't find only one line seperator from schema (${schema.lineSeparator}).",
                )

        // the lines are needed to read line by line in order to distinguish all erroneous lines
        val errors = mutableListOf<CsvExportError>()

        val iterator =
            csvMapper
                .readerFor(T::class.java)
                .with(schema)
                .readValues<T?>(csvString)

        val data =
            iterator.toDataWithErrorHandling { index, e ->
                val context = runCatching { csvString.split(lineSeparator)[index] }.getOrNull()

                when (e) {
                    is InvalidFormatException -> errors.add(InvalidFormatCsvExportError(index, context, e))
                    else -> errors.add(SimpleCsvExportError(index, context, e))
                }
            }

        if (errors.isEmpty()) {
            return Pair(data, emptyList())
        }

        // add all errors to log
        errors.forEachIndexed { i, error ->
            event.add("serialization.error[$i].index" to i)
            for (kvp in error.keyValues) {
                event.add("serialization.error[$i].${kvp.key}" to kvp.value)
            }
        }

        return Pair(data, errors)
    }

    /**
     * Converts retrieved String response into a list that is the type of Body.
     */
    inline fun <reified T> convertCsvToData(csvString: String): List<T> {
        val (data, errors) = safeConvertCsvToData<T>(csvString)
        if (errors.isNotEmpty()) {
            throw RuntimeException(
                "Unable to convert string to csv, because the string had ${errors.count()} error(s).",
            )
        }

        return data
    }
}

/** Returns the only element in the object or null */
fun onlyOrNull(list: CharArray): Char? = if (list.isEmpty() || list.size != 1) null else list[0]

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
