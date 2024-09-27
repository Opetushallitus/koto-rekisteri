package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param nimi
 * @param henkilötunnus
 * @param kansalaisuus
 * @param sukupuoli
 * @param tutkinnonSuorittamispaikka
 * @param tutkintokieli
 * @param saadutTaitotasoarviot
 * @param tutkintokertojenAjankohta
 * @param tarpeellisetYhteystiedot
 */
data class YkiSuoritus(
    @get:JsonProperty("nimi") val nimi: kotlin.String? = null,
    @get:JsonProperty("henkilötunnus") val henkilötunnus: kotlin.String? = null,
    @get:JsonProperty("kansalaisuus") val kansalaisuus: kotlin.String? = null,
    @get:JsonProperty("sukupuoli") val sukupuoli: kotlin.String? = null,
    @get:JsonProperty("tutkinnonSuorittamispaikka") val tutkinnonSuorittamispaikka: kotlin.String? = null,
    @get:JsonProperty("tutkintokieli") val tutkintokieli: kotlin.String? = null,
    @get:JsonProperty("saadutTaitotasoarviot") val saadutTaitotasoarviot: kotlin.String? = null,
    @get:JsonProperty("tutkintokertojenAjankohta") val tutkintokertojenAjankohta: kotlin.String? = null,
    @get:JsonProperty("tarpeellisetYhteystiedot") val tarpeellisetYhteystiedot: kotlin.String? = null,
)
