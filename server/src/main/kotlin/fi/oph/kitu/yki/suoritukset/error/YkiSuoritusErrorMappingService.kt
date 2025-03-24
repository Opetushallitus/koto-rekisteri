package fi.oph.kitu.yki.suoritukset.error

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.getValueOrEmpty
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.reflect.full.findAnnotation

@Service
class YkiSuoritusErrorMappingService(
    val objectMapper: ObjectMapper,
) {
    fun <Key, Value> convertListPairToJson(list: List<Pair<Key, Value>>): String =
        objectMapper.writeValueAsString(list.map { mapOf(it.first to it.second.toString()) })

    fun <Key, Value> convertJsonToListPair(json: String) =
        objectMapper.readValue<List<Map<Key, Value>>>(json).flatMap { map ->
            map.entries.map { entry -> entry.toPair() }
        }

    private final inline fun <reified T> mapCsvWithClass(csv: String): List<Pair<String?, String?>> {
        val orderedPropertiesAnnotation =
            T::class.findAnnotation<JsonPropertyOrder>()
                ?: TODO("Currently only iterating through @JsonPropertyOrder - annotation is supported.")

        val orderedProperties = orderedPropertiesAnnotation.value.toList()
        val data = csv.split(",")

        return List(maxOf(data.size, orderedProperties.size)) { index ->
            val first = orderedProperties.getOrNull(index)
            val second = data.getOrNull(index)

            first to second
        }
    }

    fun getListPairBySourceType(
        sourceType: String,
        csv: String,
    ): List<Pair<String?, String?>> =
        when (sourceType) {
            YkiSuoritusCsv::class.simpleName!! -> mapCsvWithClass<YkiSuoritusCsv>(csv)
            else -> TODO("source type '$sourceType' is not implemented yet.")
        }

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
            keyValues = convertListPairToJson(data.keyValues),
            sourceType = T::class.simpleName!!,
        )

    fun convertEntityToRowIterable(iterable: Iterable<YkiSuoritusErrorEntity>) = iterable.map { convertEntityToRow(it) }

    fun convertEntityToRow(entity: YkiSuoritusErrorEntity): YkiSuoritusErrorRow {
        val keyValues = convertJsonToListPair<String, String>(entity.keyValues)
        val csvData = getListPairBySourceType(entity.sourceType, entity.context)

        return YkiSuoritusErrorRow(
            oid = csvData.getValueOrEmpty("suorittajanOID"),
            hetu = csvData.getValueOrEmpty("hetu"),
            // TODO: There should be a more common way to convert sukinimi and etunimet into a full name.
            //  Because other UI elements may handle naming differently and therefore show the result differently.
            nimi = csvData.getValueOrEmpty("sukunimi") + " " + csvData.getValueOrEmpty("etunimet"),
            virheellinenKentta = keyValues.getValueOrEmpty("field"),
            virheellinenArvo = keyValues.getValueOrEmpty("value"),
            virheellinenSarake = entity.context,
            virheenLuontiaika = entity.created,
        )
    }
}
