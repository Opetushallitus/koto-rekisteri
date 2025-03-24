package fi.oph.kitu.yki.suoritukset.error

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.getValueOrNull
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.reflect.full.findAnnotation

@Service
class YkiSuoritusErrorMappingService(
    val objectMapper: ObjectMapper,
) {
    final inline fun <reified T> convertToEntityIterable(
        iterable: Iterable<CsvExportError>,
        created: Instant = Instant.now(),
    ) = iterable.map { convertToEntity<T>(it, created) }

    final inline fun <reified T> convertToEntity(
        data: CsvExportError,
        created: Instant = Instant.now(),
    ): YkiSuoritusErrorEntity =
        YkiSuoritusErrorEntity(
            id = null,
            message = data::class.simpleName!!,
            context = data.context!!,
            exceptionMessage = data.exception.message!!,
            stackTrace = data.exception.stackTrace!!.joinToString("\n"),
            created = created,
            keyValues =
                objectMapper.writeValueAsString(
                    data.keyValues.map { mapOf(it.first to it.second.toString()) },
                ),
            sourceType = T::class.simpleName!!,
        )

    fun convertEntityToRowIterable(iterable: Iterable<YkiSuoritusErrorEntity>) = iterable.map { convertEntityToRow(it) }

    final inline fun <reified T> mapCsvWithClass(csv: String): List<Pair<String?, String?>> {
        val orderedPropertiesAnnotation =
            T::class.findAnnotation<JsonPropertyOrder>()
                ?: TODO("Currently only iterating through @JsonPropertyOrder - annotation is supported.")

        val orderedProperties = orderedPropertiesAnnotation.value.toList()
        val data = csv.split(",")
        val resultSize = maxOf(data.size, orderedProperties.size)
        val result =
            List(resultSize) { index ->
                val first = orderedProperties.getOrNull(index)
                val second = data.getOrNull(index)

                first to second
            }

        return result
    }

    fun convertEntityToRow(entity: YkiSuoritusErrorEntity): YkiSuoritusErrorRow {
        val keyValues =
            objectMapper
                .readValue<List<Map<String, String>>>(entity.keyValues)
                .flatMap { map -> map.entries.map { entry -> entry.toPair() } }

        val csvData =
            when (entity.sourceType) {
                YkiSuoritusCsv::class.simpleName!! -> mapCsvWithClass<YkiSuoritusCsv>(entity.context)
                else -> TODO()
            }

        return YkiSuoritusErrorRow(
            oid = csvData.getValueOrNull("suorittajanOID") ?: "",
            hetu = csvData.getValueOrNull("hetu") ?: "",
            // TODO: We should probably use more common name generator and not hardcoded one
            nimi = csvData.getValueOrNull("sukunimi") + " " + csvData.getValueOrNull("etunimet"),
            virheellinenArvo = keyValues.getValueOrNull("value") ?: "",
            virheellinenSarake = entity.context,
            virheenLuontiaika = entity.created,
        )
    }
}
