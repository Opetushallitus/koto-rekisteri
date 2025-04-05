package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import kotlin.reflect.full.findAnnotation

@Service
class CsvParser(
    final val tracer: Tracer,
) {
    val columnSeparator: Char = ','
    val lineSeparator: String = "\n"
    var useHeader: Boolean = false
    val quoteChar: Char = '"'

    fun withUseHeader(value: Boolean): CsvParser =
        CsvParser(tracer).also {
            it.useHeader = value
        }

    init {
        tracer
            .spanBuilder("CsvParser.init")
            .startSpan()
            .use { span ->
                span.setAttribute("serialization.schema.args.columnSeparator", columnSeparator.toString())
                span.setAttribute("serialization.schema.args.lineSeparator", lineSeparator)
                span.setAttribute("serialization.schema.args.useHeader", useHeader)
                span.setAttribute("serialization.schema.args.quoteChar", quoteChar.toString())
            }
    }

    final inline fun <reified T> getSchema(csvMapper: CsvMapper): CsvSchema =
        tracer
            .spanBuilder("CsvParser.getSchema")
            .startSpan()
            .use { span ->
                span.setAttribute("serialization.schema.args.type", T::class.java.name)

                return@use csvMapper
                    .typedSchemaFor(T::class.java)
                    .withColumnSeparator(columnSeparator)
                    .withLineSeparator(lineSeparator)
                    .withUseHeader(useHeader)
                    .withQuoteChar(quoteChar)
            }

    final inline fun <reified T> CsvMapper.Builder.withFeatures(): CsvMapper.Builder {
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

    final inline fun <reified T> getCsvMapper() =
        CsvMapper
            .builder()
            .withFeatures<T>()
            .build()
            .withModules()

    final inline fun <reified T> streamDataAsCsv(
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
    final inline fun <reified T> convertCsvToData(csvString: String): List<TypedResult<T, CsvExportError>> =
        tracer
            .spanBuilder("CsvParser.convertCsvToData")
            .startSpan()
            .use { span ->
                span.setAttribute("serialization.isEmptyList", csvString.isBlank())
                if (csvString.isBlank()) {
                    return@use emptyList()
                }

                val csvMapper = getCsvMapper<T>()
                val schema = getSchema<T>(csvMapper)
                val lineSeparator =
                    onlyOrNull(schema.lineSeparator)
                        // This error would be probably due to internal changes so we don't pass it to normal error handling.
                        ?: throw IllegalStateException(
                            "Can't find only one line seperator from schema (${schema.lineSeparator}).",
                        )

                return@use csvMapper
                    .readerFor(T::class.java)
                    .with(schema)
                    .readValues<T?>(csvString)
                    .toTypedResults { index, e ->
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

fun <Value, Error> MappingIterator<Value>.toTypedResults(
    mapFailure: (index: Int, exception: Throwable) -> Error,
): List<TypedResult<Value, Error>> {
    val data = mutableListOf<TypedResult<Value, Error>>()
    var index = 0

    while (this.hasNext()) {
        val result =
            TypedResult
                .runCatching { this.nextValue() }
                .mapFailure { e -> mapFailure(index, e) }
                .also { index++ }

        data.add(result)
    }

    return data
}
