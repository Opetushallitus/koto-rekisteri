package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.csvparsing.CsvExportError
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class YkiSuoritusErrorMappingService {
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
            stackTrace = data.exception.stackTrace!!.toString(),
            created = created,
            sourceType = T::class.simpleName!!,
        )
}
