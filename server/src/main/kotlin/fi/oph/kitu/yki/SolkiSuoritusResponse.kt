package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import fi.oph.kitu.csvparsing.Features
import java.util.Date

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
@Features(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
class SolkiSuoritusResponse(
    @JsonProperty("suorittajanOppijanumero")
    val suorittajanOppijanumero: String,
    @JsonProperty("sukunimi")
    val sukunimi: String,
    @JsonProperty("etunimet")
    val etunimet: String,
    @JsonProperty("tutkintopaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: Date,
    @JsonProperty("tutkintokieli")
    val tutkintokieli: Tutkintokieli,
    @JsonProperty("tutkintotaso")
    val tutkintotaso: Tutkintotaso,
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
) {
    fun toEntity(id: Number? = null) =
        YkiSuoritusEntity(
            id,
            suorittajanOppijanumero,
            sukunimi,
            etunimet,
            tutkintopaiva,
            tutkintokieli,
            tutkintotaso,
            jarjestajanTunnusOid,
            jarjestajanNimi,
            tekstinYmmartaminen,
            kirjoittaminen,
            rakenteetJaSanasto,
            puheenYmmartaminen,
            puhuminen,
            yleisarvosana,
        )
}
