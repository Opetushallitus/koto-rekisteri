package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * KOTO-oppija
 * @param oid
 * @param firstName
 * @param lastName
 * @param hetu
 * @param id
 * @param nationality
 * @param gender
 * @param address
 * @param postalCode
 * @param city
 * @param email
 */
data class Oppija(
    @get:JsonProperty("oid", required = true) val oid: kotlin.String,
    @get:JsonProperty("firstName", required = true) val firstName: kotlin.String,
    @get:JsonProperty("lastName", required = true) val lastName: kotlin.String,
    @get:JsonProperty("hetu", required = true) val hetu: kotlin.String,
    @get:JsonProperty("id") val id: kotlin.Long? = null,
    @get:JsonProperty("nationality") val nationality: kotlin.String? = null,
    @get:JsonProperty("gender") val gender: kotlin.String? = null,
    @get:JsonProperty("address") val address: kotlin.String? = null,
    @get:JsonProperty("postalCode") val postalCode: kotlin.String? = null,
    @get:JsonProperty("city") val city: kotlin.String? = null,
    @get:JsonProperty("email") val email: kotlin.String? = null,
)
