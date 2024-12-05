package fi.oph.kitu.oppijanumero

import fi.oph.kitu.logging.add
import org.slf4j.spi.LoggingEventBuilder

fun LoggingEventBuilder.addOppijanumeroExceptions(exceptions: List<OppijanumeroException>) {
    exceptions.forEachIndexed { index, ex ->
        this.add(
            "error[$index].index" to index,
            "error[$index].statusCode" to ex.statusCode,
            "error[$index].oppija.kutsumanimi" to ex.oppija.kutsumanimi,
            "error[$index].oppija.etunimet" to ex.oppija.etunimet,
            "error[$index].oppija.sukunimi" to ex.oppija.sukunimi,
            "error[$index].oppija.hetu" to ex.oppija.hetu,
            "error[$index].oppija.henkilo_oid" to ex.oppija.henkilo_oid,
            "error[$index].oppija.oppijanumero" to ex.oppija.oppijanumero,
        )
    }
}
