package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param oid
 * @param sukunimi
 * @param etunimi
 * @param henkilotunnus
 * @param kansalaisuus
 * @param sukupuoli N=Nainen, M=Mies, E=muu/ei ilmoiteta
 */
data class YkiOsallistuja(
    @get:JsonProperty("oid") val oid: kotlin.String? = null,
    @get:JsonProperty("sukunimi") val sukunimi: kotlin.String? = null,
    @get:JsonProperty("etunimi") val etunimi: kotlin.String? = null,
    @get:JsonProperty("henkilotunnus") val henkilotunnus: kotlin.String? = null,
    @get:JsonProperty("kansalaisuus") val kansalaisuus: kotlin.String? = null,
    @get:JsonProperty("sukupuoli") val sukupuoli: kotlin.String? = null,
)
