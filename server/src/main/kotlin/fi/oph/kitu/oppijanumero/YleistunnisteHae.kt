package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.annotation.JsonProperty

data class YleistunnisteHaeRequest(
    @JsonProperty("etunimet")
    val etunimet: String,
    @JsonProperty("hetu")
    val hetu: String,
    @JsonProperty("kutsumanimi")
    val kutsumanimi: String,
    @JsonProperty("sukunimi")
    val sukunimi: String,
)

data class YleistunnisteHaeResponse(
    @JsonProperty("oid")
    val oid: String,
    @JsonProperty("oppijanumero")
    val oppijanumero: String?,
)
