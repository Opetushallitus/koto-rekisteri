package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kotlin.reflect.full.findAnnotation

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
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): CsvSchema {
    val schema =
        csvMapper
            .typedSchemaFor(T::class.java)
            .withColumnSeparator(columnSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)

    return schema
}
