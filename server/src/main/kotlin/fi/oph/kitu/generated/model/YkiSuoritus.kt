package fi.oph.kitu.generated.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param osallistuja
 * @param jarjestajaOid
 * @param tekstinYmmartamisenArvosana
 * @param kirjoittamisenArvosana
 * @param rakenteidenJaSanastonArvosana
 * @param puheenYmmartamisenArvosana
 * @param puhumisenArvosana
 * @param yleistasoarvio
 */
data class YkiSuoritus(
    @get:JsonProperty("osallistuja") val osallistuja: YkiOsallistuja? = null,
    @get:JsonProperty("jarjestajaOid") val jarjestajaOid: YkiJarjestaja? = null,
    @get:JsonProperty("tekstinYmmartamisenArvosana") val tekstinYmmartamisenArvosana: YkiArviointi? = null,
    @get:JsonProperty("kirjoittamisenArvosana") val kirjoittamisenArvosana: YkiArviointi? = null,
    @get:JsonProperty("rakenteidenJaSanastonArvosana") val rakenteidenJaSanastonArvosana: YkiArviointi? = null,
    @get:JsonProperty("puheenYmmartamisenArvosana") val puheenYmmartamisenArvosana: YkiArviointi? = null,
    @get:JsonProperty("puhumisenArvosana") val puhumisenArvosana: YkiArviointi? = null,
    @get:JsonProperty("yleistasoarvio") val yleistasoarvio: YkiArviointi? = null,
)
