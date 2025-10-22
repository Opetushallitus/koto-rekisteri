package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date

interface OppijanumerorekisteriRequest

class EmptyRequest : OppijanumerorekisteriRequest

data class YleistunnisteHaeRequest(
    @param:JsonProperty("etunimet")
    val etunimet: String,
    @param:JsonProperty("hetu")
    val hetu: String,
    @param:JsonProperty("kutsumanimi")
    val kutsumanimi: String,
    @param:JsonProperty("sukunimi")
    val sukunimi: String,
) : OppijanumerorekisteriRequest

data class YleistunnisteHaeResponse(
    @param:JsonProperty("oid")
    val oid: String,
    @param:JsonProperty("oppijanumero")
    val oppijanumero: String?,
)

data class OppijanumeroServiceError(
    @param:JsonProperty("timestamp")
    val timestamp: Date,
    @param:JsonProperty("status")
    val status: Int,
    @param:JsonProperty("error")
    val error: String,
    @param:JsonProperty("path")
    val path: String,
)
