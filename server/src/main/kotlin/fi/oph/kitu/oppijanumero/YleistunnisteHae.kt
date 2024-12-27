package fi.oph.kitu.oppijanumero

import java.util.Date

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

data class OppijanumeroServiceError(
    val timestamp: Date,
    val status: Int,
    val error: String,
    val path: String,
)
