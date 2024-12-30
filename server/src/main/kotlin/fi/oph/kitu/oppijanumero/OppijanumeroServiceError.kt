package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date

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
