package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date

interface OppijanumerorekisteriRequest

class EmptyRequest : OppijanumerorekisteriRequest

data class YleistunnisteHaeRequest(
    @JsonProperty("etunimet")
    val etunimet: String,
    @JsonProperty("hetu")
    val hetu: String,
    @JsonProperty("kutsumanimi")
    val kutsumanimi: String,
    @JsonProperty("sukunimi")
    val sukunimi: String,
) : OppijanumerorekisteriRequest

data class YleistunnisteHaeResponse(
    @JsonProperty("oid")
    val oid: String,
    @JsonProperty("oppijanumero")
    val oppijanumero: String?,
)

data class OppijanumeroServiceError(
    @JsonProperty("timestamp")
    val timestamp: Date,
    @JsonProperty("status")
    val status: Int,
    @JsonProperty("error")
    val error: String,
    @JsonProperty("path")
    val path: String,
)
