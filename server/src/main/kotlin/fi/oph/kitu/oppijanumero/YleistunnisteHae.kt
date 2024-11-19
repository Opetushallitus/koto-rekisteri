package fi.oph.kitu.oppijanumero

data class YleistunnisteHaeRequest(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
)

data class YleistunnisteHaeResponse(
    val oid: String,
    val oppijanumero: String?,
)
