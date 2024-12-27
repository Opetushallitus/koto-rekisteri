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

data class OppijanumeroServiceError(
    val timestamp: Int, // 1734962667439
    val status: Int, // 404
    val error: String, // "Not Found"
    val path: String, // "oppijanumerorekisteri-service/yleistunniste/hae"
)
