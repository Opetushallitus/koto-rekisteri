package fi.oph.kitu.oppijanumero

class Oppija(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
    val oppijanumero: String? = null,
    val henkilo_oid: String? = null,
) {
    fun withYleistunnisteHaeResponse(response: YleistunnisteHaeResponse) =
        Oppija(etunimet, hetu, kutsumanimi, sukunimi, response.oppijanumero, henkilo_oid = response.oid)

    fun toYleistunnisteHaeRequest() =
        YleistunnisteHaeRequest(
            etunimet,
            hetu,
            kutsumanimi,
            sukunimi,
        )
}
