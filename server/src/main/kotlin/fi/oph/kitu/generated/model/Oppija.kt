package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * KOTO-oppija
 * @param id
 * @param name
 */
data class Oppija(
    @get:JsonProperty("id", required = true) val id: kotlin.Long,
    @get:JsonProperty("name", required = true) val name: kotlin.String,
)
