package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @param suorittajanOid
 * @param sukunimi
 * @param etunimet
 * @param tutkintopaiva
 * @param tutkintokieli ISO 639-3 - koodattu kieli, jota kielitutkinnossa testataan.
 * @param tutkintotaso Arvioitava taso. PT = perustaso. Vastaa CEFR A1/A2 KT = keskitaso. Vastaa CEFR B1/B2 YT = Ylin taso. Vastaa CEFR C1/C2
 * @param jarjestajanOid
 * @param jarjestajanNimi
 * @param tekstinYmmartamisenArvosana
 * @param kirjoittamisenArvosana
 * @param rakenteidenJaSanastonArvosana
 * @param puheenYmmartamisenArvosana
 * @param puhumisenArvosana
 * @param yleisarvosana
 */
data class YkiSuoritus(
    @get:JsonProperty("suorittajanOid") val suorittajanOid: kotlin.String? = null,
    @get:JsonProperty("sukunimi") val sukunimi: kotlin.String? = null,
    @get:JsonProperty("etunimet") val etunimet: kotlin.String? = null,
    @get:JsonProperty("tutkintopaiva") val tutkintopaiva: kotlin.String? = null,
    @get:JsonProperty("tutkintokieli") val tutkintokieli: YkiSuoritus.Tutkintokieli? = null,
    @get:JsonProperty("tutkintotaso") val tutkintotaso: YkiSuoritus.Tutkintotaso? = null,
    @get:JsonProperty("jarjestajanOid") val jarjestajanOid: kotlin.String? = null,
    @get:JsonProperty("jarjestajanNimi") val jarjestajanNimi: kotlin.String? = null,
    @get:JsonProperty("tekstinYmmartamisenArvosana") val tekstinYmmartamisenArvosana: java.math.BigDecimal? = null,
    @get:JsonProperty("kirjoittamisenArvosana") val kirjoittamisenArvosana: java.math.BigDecimal? = null,
    @get:JsonProperty("rakenteidenJaSanastonArvosana") val rakenteidenJaSanastonArvosana: java.math.BigDecimal? = null,
    @get:JsonProperty("puheenYmmartamisenArvosana") val puheenYmmartamisenArvosana: java.math.BigDecimal? = null,
    @get:JsonProperty("puhumisenArvosana") val puhumisenArvosana: java.math.BigDecimal? = null,
    @get:JsonProperty("yleisarvosana") val yleisarvosana: java.math.BigDecimal? = null,
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

    /**
     * Arvioitava taso. PT = perustaso. Vastaa CEFR A1/A2 KT = keskitaso. Vastaa CEFR B1/B2 YT = Ylin taso. Vastaa CEFR C1/C2
     * Values: PT,KT,YT
     */
    enum class Tutkintotaso(
        @get:JsonValue val value: kotlin.String,
    ) {
        PT("PT"),
        KT("KT"),
        YT("YT"),
        ;

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Tutkintotaso = values().first { it -> it.value == value }
        }
    }
}
