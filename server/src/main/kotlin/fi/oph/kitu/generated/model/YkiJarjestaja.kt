package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param oid
 * @param nimi
 */
data class YkiJarjestaja(
    @get:JsonProperty("oid") val oid: kotlin.String? = null,
    @get:JsonProperty("nimi") val nimi: kotlin.String? = null,
)
