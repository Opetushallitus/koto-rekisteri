package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder(
    "suorittajanOppijanumero",
    "sukunimi",
    "etunimet",
    "tutkintopaiva",
    "tutkintokieli",
    "tutkintotaso",
    "jarjestajanTunnusOid",
    "jarjestajanNimi",
    "tekstinYmmartaminen",
    "kirjoittaminen",
    "rakenteetJaSanasto",
    "puheenYmmartaminen",
    "puhuminen",
    "yleisarvosana",
)
data class YkiSuoritus(
    @JsonProperty("suorittajanOppijanumero")
    val suorittajanOppijanumero: String,
    @JsonProperty("sukunimi")
    val sukunimi: String,
    @JsonProperty("etunimet")
    val etunimet: String,
    @JsonProperty("tutkintopaiva")
    val tutkintopaiva: String, // ISO-8601-muodossa
    @JsonProperty("tutkintokieli")
    val tutkintokieli: String, // ISO 649-2 alpha-3 -muodossa
    @JsonProperty("tutkintotaso")
    val tutkintotaso: String, // ("PT"=perustaso, "KT"=keskitaso, "YT"=ylin taso)
    @JsonProperty("jarjestajanTunnusOid")
    val jarjestajanTunnusOid: String,
    @JsonProperty("jarjestajanNimi")
    val jarjestajanNimi: String,
    @JsonProperty("tekstinYmmartaminen")
    val tekstinYmmartaminen: Number,
    @JsonProperty("kirjoittaminen")
    val kirjoittaminen: Number,
    @JsonProperty("rakenteetJaSanasto")
    val rakenteetJaSanasto: Number,
    @JsonProperty("puheenYmmartaminen")
    val puheenYmmartaminen: Number,
    @JsonProperty("puhuminen")
    val puhuminen: Number,
    @JsonProperty("yleisarvosana")
    val yleisarvosana: Number,
)
