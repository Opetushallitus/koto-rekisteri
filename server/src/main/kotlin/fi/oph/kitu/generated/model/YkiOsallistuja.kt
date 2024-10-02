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
 * @param katuosoite Osallistujan tarpeellisten yhteystietojen katuosoite
 * @param postinumero Osallistujan tarpeellisten yhteystietojen postinumero
 * @param postitoimipaikka Osallistujan tarpeellisten yhteystietojen postitoimipaikka
 * @param sähköpostiosoite Osallistujan tarpeellisten yhteystietojen sähköpostiosoite
 */
data class YkiOsallistuja(
    @get:JsonProperty("oid") val oid: kotlin.String? = null,
    @get:JsonProperty("sukunimi") val sukunimi: kotlin.String? = null,
    @get:JsonProperty("etunimi") val etunimi: kotlin.String? = null,
    @get:JsonProperty("henkilotunnus") val henkilotunnus: kotlin.String? = null,
    @get:JsonProperty("kansalaisuus") val kansalaisuus: kotlin.String? = null,
    @get:JsonProperty("sukupuoli") val sukupuoli: kotlin.String? = null,
    @get:JsonProperty("katuosoite") val katuosoite: kotlin.String? = null,
    @get:JsonProperty("postinumero") val postinumero: kotlin.String? = null,
    @get:JsonProperty("postitoimipaikka") val postitoimipaikka: kotlin.String? = null,
    @get:JsonProperty("sähköpostiosoite") val sähköpostiosoite: kotlin.String? = null,
)
