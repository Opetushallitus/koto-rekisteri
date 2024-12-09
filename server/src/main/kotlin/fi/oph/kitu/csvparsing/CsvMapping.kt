package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.oph.kitu.logging.add
import org.ietf.jgss.Oid
import org.slf4j.spi.LoggingEventBuilder
import kotlin.reflect.full.findAnnotation

data class CsvArgs(
    val event: LoggingEventBuilder,
    val columnSeparator: Char = ',',
    val lineSeparator: String = "\n",
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
    val event = args.event
    event.add(
        "serialization.schema.args.type" to T::class.java.name,
        "serialization.schema.args.columnSeparator" to args.columnSeparator.toString(),
        "serialization.schema.args.useHeader" to args.useHeader,
        "serialization.schema.args.quoteChar" to args.quoteChar,
    )

    val schema: CsvSchema =
        csvMapper
            .typedSchemaFor(T::class.java)
            .withColumnSeparator(args.columnSeparator)
            .withLineSeparator(args.lineSeparator)
            .withUseHeader(args.useHeader)
            .withQuoteChar(args.quoteChar)

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
