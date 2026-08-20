package fi.oph.kitu.csvparsing

import fi.oph.kitu.observability.use
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.oid.OidDeserializer
import fi.oph.kitu.oid.OidSerializer
import io.opentelemetry.api.trace.Tracer
import org.springframework.stereotype.Service
import tools.jackson.databind.module.SimpleModule
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.dataformat.csv.CsvSchema
import java.io.ByteArrayOutputStream

@Service
class CsvGenerator(
    final val tracer: Tracer,
) {
    var columnSeparator: Char = ','
    val lineSeparator: String = "\n"
    var useHeader: Boolean = false
    val quoteChar: Char = '"'

    fun withUseHeader(value: Boolean): CsvGenerator =
        CsvGenerator(tracer).also {
            it.useHeader = value
            it.columnSeparator = columnSeparator
        }

    fun withColumnSeparator(value: Char): CsvGenerator =
        CsvGenerator(tracer).also {
            it.useHeader = useHeader
            it.columnSeparator = value
        }

    init {
        tracer
            .spanBuilder("CsvGenerator.init")
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
            .spanBuilder("CsvGenerator.getSchema")
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

    final inline fun <reified T> getCsvMapper(): CsvMapper =
        CsvMapper
            .builder()
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
}
