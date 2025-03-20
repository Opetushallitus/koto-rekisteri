package fi.oph.kitu.yki.suoritukset.error

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.csvparsing.CsvExportError
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class YkiSuoritusErrorMappingService(
    private val objectMapper: ObjectMapper, 
) {
    final inline fun <reified T> convertToEntityIterable(
        iterable: Iterable<CsvExportError>,
        created: Instant = Instant.now(),
    ) = iterable.map { convertToEntity<T>(it, created) }

    final inline fun <reified T> convertToEntity(
        data: CsvExportError,
        created: Instant = Instant.now(),
    ) = YkiSuoritusErrorEntity(
        id = null,
        message = data::class.simpleName!!,
        context = data.context!!,
        exceptionMessage = data.exception.message!!,
        stackTrace = data.exception.stackTrace!!.joinToString("\n"),
        created = created,
        keyValues = objectMapper.writeValueAsString(data.keyValues),
        sourceType = T::class.simpleName!!,
    )
}
