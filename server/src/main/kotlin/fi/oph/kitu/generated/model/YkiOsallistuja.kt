package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @param oid
 * @param sukunimi
 * @param etunimi
 * @param henkilotunnus Suomalainen henkilötunnus
 * @param kansalaisuus
 * @param sukupuoli N=Nainen, M=Mies, E=muu/ei ilmoiteta
 * @param katuosoite Osallistujan tarpeellisten yhteystietojen katuosoite
 * @param postinumero Osallistujan tarpeellisten yhteystietojen postinumero
 * @param postitoimipaikka Osallistujan tarpeellisten yhteystietojen postitoimipaikka
 * @param sahköpostiosoite Osallistujan tarpeellisten yhteystietojen sähköpostiosoite
 */
data class YkiOsallistuja(
    @get:JsonProperty("oid") val oid: kotlin.String? = null,
    @get:JsonProperty("sukunimi") val sukunimi: kotlin.String? = null,
    @get:JsonProperty("etunimi") val etunimi: kotlin.String? = null,
    @get:JsonProperty("henkilotunnus") val henkilotunnus: kotlin.String? = null,
    @get:JsonProperty("kansalaisuus") val kansalaisuus: kotlin.String? = null,
    @get:JsonProperty("sukupuoli") val sukupuoli: YkiOsallistuja.Sukupuoli? = null,
    @get:JsonProperty("katuosoite") val katuosoite: kotlin.String? = null,
    @get:JsonProperty("postinumero") val postinumero: kotlin.String? = null,
    @get:JsonProperty("postitoimipaikka") val postitoimipaikka: kotlin.String? = null,
    @get:JsonProperty("sahköpostiosoite") val sahköpostiosoite: kotlin.String? = null,
) {
    /**
     * N=Nainen, M=Mies, E=muu/ei ilmoiteta
     * Values: N,M,E
     */
    enum class Sukupuoli(
        @get:JsonValue val value: kotlin.String,
    ) {
        N("N"),
        M("M"),
        E("E"),
        ;

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Sukupuoli = values().first { it -> it.value == value }
        }
    }
}
