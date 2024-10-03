package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param osallistuja
 * @param jarjestajaOid
 * @param tutkintokieli
 * @param saadutTaitotasoarviot
 * @param tutkintokertojenAjankohta
 */
data class YkiSuoritus(
    @get:JsonProperty("osallistuja") val osallistuja: YkiOsallistuja? = null,
    @get:JsonProperty("jarjestajaOid") val jarjestajaOid: YkiJarjestaja? = null,
    @get:JsonProperty("tutkintokieli") val tutkintokieli: kotlin.String? = null,
    @get:JsonProperty("saadutTaitotasoarviot") val saadutTaitotasoarviot: kotlin.String? = null,
    @get:JsonProperty("tutkintokertojenAjankohta") val tutkintokertojenAjankohta: kotlin.String? = null,
)
