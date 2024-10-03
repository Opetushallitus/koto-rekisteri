package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @param arviointinumero
 * @param pvm
 * @param tutkintokieli ISO 639-3 - koodattu kieli, jota kielitutkinnossa testataan.
 * @param taso Arvioitava taso. PT = perustaso. Vastaa CEFR A1/A2 KT = keskitaso. Vastaa CEFR B1/B2 YT = Ylin taso. Vastaa CEFR C1/C2
 * @param arvioijaOid Arvioijan OID-tunniste
 */
data class YkiArviointi(
    @get:JsonProperty("arviointinumero") val arviointinumero: kotlin.String? = null,
    @get:JsonProperty("pvm") val pvm: kotlin.String? = null,
    @get:JsonProperty("tutkintokieli") val tutkintokieli: YkiArviointi.Tutkintokieli? = null,
    @get:JsonProperty("taso") val taso: YkiArviointi.Taso? = null,
    @get:JsonProperty("arvioijaOid") val arvioijaOid: kotlin.String? = null,
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
    enum class Taso(
        @get:JsonValue val value: kotlin.String,
    ) {
        PT("PT"),
        KT("KT"),
        YT("YT"),
        ;

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Taso = values().first { it -> it.value == value }
        }
    }
}
