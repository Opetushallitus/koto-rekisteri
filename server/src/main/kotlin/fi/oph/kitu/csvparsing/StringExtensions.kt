package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.web.client.RestClientException
import kotlin.reflect.full.findAnnotation

/**
 * Converts retrieved String response into a list that is the type of Body.
 */
inline fun <reified T> String.asCsv(
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): List<T> {
    if (this.isBlank()) {
        return emptyList()
    }

    val csvMapper = CsvMapper()
    csvMapper.registerModule(JavaTimeModule())

    val mapperFeatures = T::class.findAnnotation<Features>()?.features
    if (mapperFeatures != null) {
        for (feature in mapperFeatures) {
            csvMapper.enable(feature)
        }
    }

    val schema =
        csvMapper
            .typedSchemaFor(T::class.java)
            .withColumnSeparator(columnSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)

    return try {
        csvMapper
            .readerFor(T::class.java)
            .with(schema)
            .readValues<T>(this)
            .readAll()
    } catch (e: Exception) {
        throw RestClientException("Could not parse string as CSV into a type ${T::class.java.name}.", e)
    }
}
