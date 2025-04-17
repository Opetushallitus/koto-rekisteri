package fi.oph.kitu.oppijanumero

class Oppija(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
)

data class TunnistettuOppija(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
    val oppijanumero: String,
)
