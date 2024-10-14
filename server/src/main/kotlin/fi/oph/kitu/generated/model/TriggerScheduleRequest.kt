package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param lastSeen
 */
data class TriggerScheduleRequest(
    @get:JsonProperty("lastSeen") val lastSeen: java.time.LocalDate? = null,
)
