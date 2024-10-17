package fi.oph.kitu

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

// Converts retrieved String response into a list that is the type of Body.
inline fun <reified T> String.asCsv(
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): List<T> {
    if (this.isBlank()) {
        return emptyList()
    }

    val csvMapper = CsvMapper()
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

fun RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse.isDryRun(): Boolean =
    this.headers["dry-run"]?.first()?.toBoolean() ?: false
