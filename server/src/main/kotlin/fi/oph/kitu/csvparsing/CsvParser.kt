package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.oph.kitu.logging.add
import org.ietf.jgss.Oid
import org.slf4j.spi.LoggingEventBuilder
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.reflect.full.findAnnotation

class CsvParser(
    val event: LoggingEventBuilder,
    val columnSeparator: Char = ',',
    val lineSeparator: String = "\n",
    val useHeader: Boolean = false,
    val quoteChar: Char = '"',
) {
    inline fun <reified T> getSchema(csvMapper: CsvMapper): CsvSchema {
        event.add(
            "serialization.schema.args.type" to T::class.java.name,
            "serialization.schema.args.columnSeparator" to columnSeparator.toString(),
            "serialization.schema.args.useHeader" to useHeader,
            "serialization.schema.args.quoteChar" to quoteChar,
        )

        val schema: CsvSchema =
            csvMapper
                .typedSchemaFor(T::class.java)
                .withColumnSeparator(columnSeparator)
                .withLineSeparator(lineSeparator)
                .withUseHeader(useHeader)
                .withQuoteChar(quoteChar)

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

    inline fun <reified T> streamDataAsCsv(
        outputStream: ByteArrayOutputStream,
        data: Iterable<T>,
    ) {
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
    inline fun <reified T> convertCsvToData(csvString: String): List<T> {
        if (csvString.isBlank()) {
            event.add("serialization.isEmptyList" to true)
            return emptyList()
        }

        event.add("serialization.isEmptyList" to false)

        val csvMapper = getCsvMapper<T>()
        val schema = getSchema<T>(csvMapper)

        // the lines are needed to read line by line in order to distinguish all erroneous lines
        val errors = mutableListOf<CsvExportError>()

        val iterator =
            csvMapper
                .readerFor(T::class.java)
                .with(schema)
                .readValues<T?>(csvString)

        val debugCsvString = csvString
        println(debugCsvString)

        val data = mutableListOf<T>()
        var index = 0
        while (iterator.hasNext()) {
            try {
                val row = iterator.nextValue()
                data.add(row)
            } catch (e: InvalidFormatException) {
                errors.add(InvalidFormatCsvExportError(index, e))
            } catch (e: Exception) {
                errors.add(SimpleCsvExportError(index, e))
            } finally {
                index++
            }
        }

        if (errors.isEmpty()) {
            return data
        }

        // add all errors to log
        errors.forEachIndexed { i, error ->
            event.add("serialization.error[$i].index" to i)
            for (kvp in error.keyValues) {
                event.add("serialization.error[$i].${kvp.first}" to kvp.second)
            }
        }

        throw RuntimeException("Unable to convert string to csv, because the string had ${errors.count()} error(s).")
    }
}
