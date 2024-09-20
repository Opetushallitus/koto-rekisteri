package fi.oph.kitu.oppijanumerorekisteri

data class YleistunnisteHaeRequest(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
)
