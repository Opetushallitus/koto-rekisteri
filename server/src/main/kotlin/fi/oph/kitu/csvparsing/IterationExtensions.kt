package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import java.io.StringWriter

inline fun <reified T> Iterable<T>.toCsvString(
    columnSeparator: Char = ',',
    useHeader: Boolean = false,
    quoteChar: Char = '"',
): String {
    val csvMapper = CsvMapper()
    val schema =
        csvMapper
            .typedSchemaFor(T::class.java)
            .withColumnSeparator(columnSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)

    val writer = StringWriter()

    csvMapper
        .writerFor(Iterable::class.java)
        .with(schema)
        .writeValue(writer, this)

    val str = writer.toString()
    return str
}
