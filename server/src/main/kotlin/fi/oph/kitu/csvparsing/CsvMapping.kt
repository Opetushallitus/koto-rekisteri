package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.opentelemetry.api.trace.Span
import org.ietf.jgss.Oid
import kotlin.reflect.full.findAnnotation

data class CsvArgs(
    val columnSeparator: Char = ',',
    val useHeader: Boolean = false,
    val quoteChar: Char = '"',
)

inline fun <reified T> getCsvMapper(): CsvMapper {
    val builder = CsvMapper.builder()
    val mapperFeatures = T::class.findAnnotation<Features>()?.features
    if (mapperFeatures != null) {
        for (feature in mapperFeatures) {
            builder.enable(feature)
        }
    }

    val csvMapper = builder.build()

    csvMapper.registerModule(JavaTimeModule())
    val oidSerializerModule = SimpleModule()
    oidSerializerModule.addSerializer(Oid::class.java, OidSerializer())
    csvMapper.registerModule(oidSerializerModule)

    return csvMapper
}

inline fun <reified T> getSchema(
    csvMapper: CsvMapper,
    args: CsvArgs,
): CsvSchema {
    Span.current().apply {
        setAttribute("serialization.schema.args.type", T::class.java.name)
        setAttribute("serialization.schema.args.columnSeparator", args.columnSeparator.toString())
        setAttribute("serialization.schema.args.useHeader", args.useHeader)
        setAttribute("serialization.schema.args.quoteChar", args.quoteChar.toString())
    }

    val schema: CsvSchema =
        csvMapper
            .typedSchemaFor(T::class.java)
            .withColumnSeparator(args.columnSeparator)
            .withUseHeader(args.useHeader)
            .withQuoteChar(args.quoteChar)

    schema
        .sortedBy { column -> column.index }
        .forEach { column ->
            val paddedIndex = column.index.toString().padStart(2, '0')
            Span.current().apply {
                setAttribute("serialization.schema.column[$paddedIndex].index", column.index.toLong())
                setAttribute("serialization.schema.column[$paddedIndex].name", column.name)
                setAttribute("serialization.schema.column[$paddedIndex].type", column.type.name)
            }
        }

    return schema
}
