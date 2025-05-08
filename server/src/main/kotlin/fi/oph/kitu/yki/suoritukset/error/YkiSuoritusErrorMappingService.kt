package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.csvparsing.CsvExportError
import fi.oph.kitu.csvparsing.InvalidFormatCsvExportError
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class YkiSuoritusErrorMappingService {
    @WithSpan
    fun convertToEntityIterable(iterable: Iterable<CsvExportError>) = iterable.map { convertToEntity(it) }

    /**
     * Tries to convert raw CSV data into YkiSuoritusErrorEntity as well as possible.
     *
     * The method assumes the data was tried to be converted into an YkiSuoritusEntity and the conversion failed.
     * Therefore the conversion of this function is done with the best effort. For example, it won't handle
     * values inside double quotes or commas inside quoted fields correctly.
     */
    fun convertToEntity(data: CsvExportError): YkiSuoritusErrorEntity {
        val csv = data.context!!.split(",")

        return YkiSuoritusErrorEntity(
            id = null,
            suorittajanOid = csv[0],
            hetu = csv[1],
            nimi = csv[3] + " " + csv[4],
            lastModified = runCatching { Instant.parse(csv[11]) }.getOrNull(),
            virheellinenKentta = if (data is InvalidFormatCsvExportError) data.fieldWithValidationError else null,
            virheellinenArvo = if (data is InvalidFormatCsvExportError) data.valueWithValidationError else null,
            virheellinenRivi = data.context,
            virheenRivinumero = data.keyValues["lineNumber"] as Int,
            virheenLuontiaika = Instant.now(),
        )
    }
}
