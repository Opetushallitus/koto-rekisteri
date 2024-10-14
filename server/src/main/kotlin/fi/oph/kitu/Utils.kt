package fi.oph.kitu

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

// Converts retrieved String response into a list that is the type of Body.
inline fun <reified Body> RestClient.ResponseSpec.csvBody(
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): Array<Body> {
    val csvMapper = CsvMapper()

    val schema =
        csvMapper
            .typedSchemaFor(Body::class.java)
            .withColumnSeparator(columnSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)

    return try {
        val csvString = this.body(String::class.java) ?: throw RestClientException("Response is null")

        val body =
            csvMapper
                .readerFor(Body::class.java)
                .with(schema)
                .readValue<Array<Body>>(csvString)

        body
    } catch (e: Exception) {
        throw RestClientException("Failed to parse CSV response", e)
    }
}
