package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @param osallistuja
 * @param jarjestajaOid
 * @param tutkintokieli ISO 639-3 - koodattu kieli, jota kielitutkinnossa testataan.
 * @param tekstinYmmartamisenArvosana Tekstin ymmärtämisen arvosana
 * @param kirjoittamisenArvosana Kirjoittamisen arvosana
 * @param rakenteidenJaSanastonArvosana Rakenteiden ja sanaston arvosana
 * @param puheenYmmartamisenArvosana Puheen ymmärtämisen arvosana
 * @param puhumisenArvosana Puhumisen arvosana
 * @param yleistasoarvio Yleistasoarvio
 * @param pvm Suorituksen päivämäärä
 */
data class YkiSuoritus(
    @get:JsonProperty("osallistuja") val osallistuja: YkiOsallistuja? = null,
    @get:JsonProperty("jarjestajaOid") val jarjestajaOid: YkiJarjestaja? = null,
    @get:JsonProperty("tutkintokieli") val tutkintokieli: YkiSuoritus.Tutkintokieli? = null,
    @get:JsonProperty("tekstinYmmartamisenArvosana") val tekstinYmmartamisenArvosana: kotlin.String? = null,
    @get:JsonProperty("kirjoittamisenArvosana") val kirjoittamisenArvosana: kotlin.String? = null,
    @get:JsonProperty("rakenteidenJaSanastonArvosana") val rakenteidenJaSanastonArvosana: kotlin.String? = null,
    @get:JsonProperty("puheenYmmartamisenArvosana") val puheenYmmartamisenArvosana: kotlin.String? = null,
    @get:JsonProperty("puhumisenArvosana") val puhumisenArvosana: kotlin.String? = null,
    @get:JsonProperty("yleistasoarvio") val yleistasoarvio: kotlin.String? = null,
    @get:JsonProperty("pvm") val pvm: java.time.LocalDate? = null,
) {
    /**
     * ISO 639-3 - koodattu kieli, jota kielitutkinnossa testataan.
     * Values: DEU,ENG,FIN,FRA,ITA,RUS,SME,SPA,SWE
     */
    enum class Tutkintokieli(
        @get:JsonValue val value: kotlin.String,
    ) {
        DEU("DEU"),
        ENG("ENG"),
        FIN("FIN"),
        FRA("FRA"),
        ITA("ITA"),
        RUS("RUS"),
        SME("SME"),
        SPA("SPA"),
        SWE("SWE"),
        ;

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Tutkintokieli = values().first { it -> it.value == value }
        }
    }
}
