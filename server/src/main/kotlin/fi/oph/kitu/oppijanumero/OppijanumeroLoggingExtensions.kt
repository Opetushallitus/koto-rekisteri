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
            "error.oppijanumero[$index].statusCode" to ex.statusCode,
            "error.oppijanumero[$index].oppija.kutsumanimi" to ex.oppija.kutsumanimi,
            "error.oppijanumero[$index].oppija.etunimet" to ex.oppija.etunimet,
            "error.oppijanumero[$index].oppija.sukunimi" to ex.oppija.sukunimi,
            "error.oppijanumero[$index].oppija.hetu" to ex.oppija.hetu,
            "error.oppijanumero[$index].oppija.henkilo_oid" to ex.oppija.henkilo_oid,
            "error.oppijanumero[$index].oppija.oppijanumero" to ex.oppija.oppijanumero,
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
