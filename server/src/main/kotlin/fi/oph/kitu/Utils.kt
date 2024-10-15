package fi.oph.kitu

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

// Converts retrieved String response into a list that is the type of Body.
inline fun <reified Body> RestClient.ResponseSpec.csvBody(
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): List<Body> {
    val csvMapper = CsvMapper()
    val schema =
        csvMapper
            .typedSchemaFor(Body::class.java)
            .withColumnSeparator(columnSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)

    return try {
        csvMapper
            .readerFor(Body::class.java)
            .with(schema)
            .readValues<Body>(this.body(String::class.java))
            .readAll()
    } catch (e: Exception) {
        throw RestClientException("Failed to parse CSV response", e)
    }
}
