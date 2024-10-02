package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param oid
 * @param nimi
 * @param katuosoite
 * @param postinumero
 * @param postitoimipaikka
 * @param puhelin
 * @param yhteyshenkilo
 * @param wwwosoite
 * @param tutkintotarjonta
 */
data class YkiJarjestaja(
    @get:JsonProperty("oid") val oid: kotlin.String? = null,
    @get:JsonProperty("nimi") val nimi: kotlin.String? = null,
    @get:JsonProperty("katuosoite") val katuosoite: kotlin.String? = null,
    @get:JsonProperty("postinumero") val postinumero: kotlin.String? = null,
    @get:JsonProperty("postitoimipaikka") val postitoimipaikka: kotlin.String? = null,
    @get:JsonProperty("puhelin") val puhelin: kotlin.String? = null,
    @get:JsonProperty("yhteyshenkilo") val yhteyshenkilo: kotlin.String? = null,
    @get:JsonProperty("wwwosoite") val wwwosoite: kotlin.String? = null,
    @get:JsonProperty("tutkintotarjonta") val tutkintotarjonta: kotlin.collections.List<YkiTutkintotarjonta>? = null,
)
