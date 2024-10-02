package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param kieli
 * @param taso Tutkinnon taso. PT=Perustaso, KT=keskitaso, YT=ylin taso
 */
data class YkiTutkintotarjonta(
    @get:JsonProperty("kieli") val kieli: kotlin.String? = null,
    @get:JsonProperty("taso") val taso: kotlin.String? = null,
)
