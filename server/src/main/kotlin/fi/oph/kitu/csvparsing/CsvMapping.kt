package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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

    return csvMapper
}

inline fun <reified T> getSchema(
    csvMapper: CsvMapper,
    args: CsvArgs,
) = csvMapper
    .typedSchemaFor(T::class.java)
    .withColumnSeparator(args.columnSeparator)
    .withUseHeader(args.useHeader)
    .withQuoteChar(args.quoteChar)
