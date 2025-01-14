package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.oph.kitu.logging.add
import org.ietf.jgss.Oid
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import kotlin.reflect.full.findAnnotation

@Service
class CsvParser(
    val columnSeparator: Char = ',',
    val lineSeparator: String = "\n",
    var useHeader: Boolean = false,
    val quoteChar: Char = '"',
) {
    val extensions = mutableListOf<CsvParserExtension>()

    fun with(
        columnSeparator: Char = ',',
        lineSeparator: String = "\n",
        useHeader: Boolean = false,
        quoteChar: Char = '"',
    ) = CsvParser(columnSeparator, lineSeparator, useHeader, quoteChar)

    fun withExtension(extension: CsvParserExtension) =
        CsvParser(columnSeparator, lineSeparator, useHeader, quoteChar)
            .apply { extensions += extension }

    init {
        extensions.forEach { it.onInit(this) }
    }

    final inline fun <reified T> getSchema(csvMapper: CsvMapper): CsvSchema =
        csvMapper
            .typedSchemaFor(T::class.java)
            .withColumnSeparator(columnSeparator)
            .withLineSeparator(lineSeparator)
            .withUseHeader(useHeader)
            .withQuoteChar(quoteChar)

    final inline fun <reified T> CsvMapper.Builder.withFeatures(): CsvMapper.Builder {
        val mapperFeatures = T::class.findAnnotation<Features>()?.features
        if (mapperFeatures != null) {
            for (feature in mapperFeatures) {
                this.enable(feature)
            }
        }

        return this
    }

    fun CsvMapper.withModules(): CsvMapper {
        this.registerModules(
            JavaTimeModule(),
            SimpleModule()
                .addSerializer(Oid::class.java, OidSerializer()),
        )

        return this
    }

    final inline fun <reified T> getCsvMapper(): CsvMapper =
        CsvMapper
            .builder()
            .withFeatures<T>()
            .build()
            .withModules()

    final inline fun <reified T> streamDataAsCsv(
        outputStream: ByteArrayOutputStream,
        data: Iterable<T>,
    ) {
        extensions.forEach { it.beforeFunctionCall(this, null, T::class.java) }

        val csvMapper: CsvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)

        csvMapper
            .writerFor(Iterable::class.java)
            .with(schema)
            .writeValue(outputStream, data)
    }

    /**
     * Converts retrieved String response into a list that is the type of Body.
     */
    final inline fun <reified T> convertCsvToData(csvString: String): List<T> {
        extensions.forEach { it.beforeFunctionCall(this, csvString, T::class.java) }

        if (csvString.isBlank()) {
            return emptyList()
        }

        val csvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)

        // the lines are needed to read line by line in order to distinguish all erroneous lines
        val errors = mutableListOf<CsvExportError>()

        val iterator =
            csvMapper
                .readerFor(T::class.java)
                .with(schema)
                .readValues<T?>(csvString)

        val data =
            iterator.toDataWithErrorHandling { index, e ->
                when (e) {
                    is InvalidFormatException -> errors.add(InvalidFormatCsvExportError(index, e))
                    else -> errors.add(SimpleCsvExportError(index, e))
                }
            }

        if (errors.isNotEmpty()) {
            throw CsvExportException(errors).also { ex ->
                extensions.forEach { it.onErrorFunctionCall(this, ex) }
            }
        }

        return data
    }
}

fun <T> MappingIterator<T>.toDataWithErrorHandling(
    onFailure: (index: Int, exception: Throwable) -> Unit = { _, _ -> },
): List<T> {
    val data = mutableListOf<T>()
    var index = 0
    while (this.hasNext()) {
        runCatching { data.add(this.nextValue()) }
            .onFailure { e -> onFailure(index, e) }
            .also { index++ }
    }

    return data
}
