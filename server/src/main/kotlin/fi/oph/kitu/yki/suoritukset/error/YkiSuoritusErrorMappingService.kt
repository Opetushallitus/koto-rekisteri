package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.csvparsing.CsvExportError
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class YkiSuoritusErrorMappingService {
    fun convertToEntityIterable(
        iterable: Iterable<CsvExportError>,
        created: Instant = Instant.now(),
    ) = iterable.map { convertToEntity(it, created) }

    fun convertToEntity(
        data: CsvExportError,
        created: Instant = Instant.now(),
    ): YkiSuoritusErrorEntity {
        val csv = data.context!!.split(",")

        return YkiSuoritusErrorEntity(
            id = null,
            oid = csv[0],
            hetu = csv[1],
            nimi = csv[3] + " " + csv[4],
            lastModified = runCatching { Instant.parse(csv[11]) }.getOrNull(),
            virheellinenKentta = data.keyValues["field"]?.toString(),
            virheellinenArvo = data.keyValues["value"]?.toString(),
            virheellinenRivi = data.context,
            virheenRivinumero = data.keyValues["lineNumber"] as Int,
            virheenLuontiaika = Instant.now(),
        )
    }
}
