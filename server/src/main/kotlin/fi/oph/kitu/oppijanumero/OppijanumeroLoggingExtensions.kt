package fi.oph.kitu.oppijanumero

import fi.oph.kitu.kotoutumiskoulutus.KoealustaMappingService
import fi.oph.kitu.logging.add
import org.slf4j.spi.LoggingEventBuilder

fun LoggingEventBuilder.addValidationExceptions(
    oppijanumeroExceptions: List<OppijanumeroException>,
    validationExceptions: Iterable<KoealustaMappingService.Error.Validation>,
) {
    oppijanumeroExceptions.forEachIndexed { index, ex ->
        this.add(
            "error.oppijanumero[$index].index" to index,
            "error.oppijanumero[$index].oppija.kutsumanimi" to ex.request.kutsumanimi,
            "error.oppijanumero[$index].oppija.etunimet" to ex.request.etunimet,
            "error.oppijanumero[$index].oppija.sukunimi" to ex.request.sukunimi,
            "error.oppijanumero[$index].oppija.hetu" to ex.request.hetu,
        )
    }
    validationExceptions.forEachIndexed { index, ex ->
        this.add(
            "error.validation[$index].index" to index,
            "error.validation[$index].userid" to ex.userId,
            "error.validation[$index].message" to ex.message,
        )
    }
}
