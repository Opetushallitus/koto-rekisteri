package fi.oph.kitu.csvparsing

import arrow.core.Either
import arrow.core.left
import fi.oph.kitu.observability.use
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oid.OidDeserializer
import fi.oph.kitu.oid.OidSerializer
import io.opentelemetry.api.trace.Tracer
import org.springframework.stereotype.Service
import tools.jackson.databind.MappingIterator
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.module.SimpleModule
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.dataformat.csv.CsvSchema
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
        val mapperFeatures = T::class.findAnnotation<MapperFeatures>()?.features
        if (mapperFeatures != null) {
            for (feature in mapperFeatures) {
                this.enable(feature)
            }
        }

        return this
    }

    // Jackson 3 auto-registers java.time support; only app-specific modules go here.
    val oidModule: SimpleModule
        get() =
            SimpleModule()
                .addSerializer(Oid::class.java, OidSerializer())
                .addDeserializer(Oid::class.java, OidDeserializer())

    // Neutralizes spreadsheet-formula triggers on String exports.
    // Only affects writing — readers use Jackson's default String deserializer.
    val csvFormulaSafeStringModule: SimpleModule
        get() =
            SimpleModule()
                .addSerializer(String::class.java, CsvFormulaSafeStringSerializer())

    final inline fun <reified T> getCsvMapper() =
        CsvMapper
            .builder()
            .withFeatures<T>()
            .addModule(oidModule)
            .addModule(csvFormulaSafeStringModule)
            .build()

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
    final inline fun <reified T> convertCsvToData(csvString: String): List<Either<CsvExportError, T>> =
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
                        ?: return@use listOf(
                            SimpleCsvExportError(
                                lineNumber = 0,
                                context = null,
                                exception =
                                    IllegalStateException(
                                        "Can't find only one line seperator from schema (${schema.lineSeparator}).",
                                    ),
                            ).left(),
                        )

                return@use csvMapper
                    .readerFor(T::class.java)
                    .with(schema)
                    .readValues<T>(csvString)
                    .toEithers { index, e ->
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

fun <Value, Error> MappingIterator<Value>.toEithers(
    mapFailure: (index: Int, exception: Throwable) -> Error,
): List<Either<Error, Value>> {
    val data = mutableListOf<Either<Error, Value>>()
    var index = 0

    while (this.hasNext()) {
        val result =
            Either
                .catch { this.nextValue() }
                .mapLeft { e -> mapFailure(index, e) }
                .also { index++ }

        data.add(result)
    }

    return data
}
