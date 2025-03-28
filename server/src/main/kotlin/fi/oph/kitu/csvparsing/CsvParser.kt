package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.oph.kitu.InclusiveTypedResult
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
     */
    inline fun <reified T> convertCsvToData(csvString: String): List<InclusiveTypedResult<T, CsvExportError>> {
        if (csvString.isBlank()) {
            event.add("serialization.isEmptyList" to true)
            return emptyList()
        }

        event.add("serialization.isEmptyList" to false)

        val csvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)
        val lineSeparator =
            onlyOrNull(schema.lineSeparator)
                // This error would be probably due to internal changes so we don't pass it to normal error handling.
                ?: throw IllegalStateException(
                    "Can't find only one line seperator from schema (${schema.lineSeparator}).",
                )

        return csvMapper
            .readerFor(T::class.java)
            .with(schema)
            .readValues<T?>(csvString)
            .toInclusiveTypedResults { index, e ->
                val context = runCatching { csvString.split(lineSeparator)[index] }.getOrNull()

                when (e) {
                    is InvalidFormatException -> InvalidFormatCsvExportError(index, context, e)
                    else -> SimpleCsvExportError(index, context, e)
                }
            }
    }
}

/** Returns the only element in the object or null */
fun onlyOrNull(list: CharArray): Char? = if (list.isEmpty() || list.size != 1) null else list[0]

fun <Value, Error> MappingIterator<Value>.toInclusiveTypedResults(
    mapFailure: (index: Int, exception: Throwable) -> Error,
): List<InclusiveTypedResult<Value, Error>> {
    val data = mutableListOf<InclusiveTypedResult<Value, Error>>()
    var index = 0

    while (this.hasNext()) {
        val result =
            InclusiveTypedResult
                .runCatching { this.nextValue() }
                .mapFailure { e -> mapFailure(index, e) }
                .also { index++ }

        data.add(result)
    }

    return data
}
