package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import fi.oph.kitu.csvparsing.Features
import org.ietf.jgss.Oid
import java.util.Date

@JsonPropertyOrder(
    "suorittajanOppijanumero",
    "hetu",
    "sukupuoli",
    "sukunimi",
    "etunimet",
    "kansalaisuus",
    "katuosoite",
    "postinumero",
    "postitoimipaikka",
    "email",
    "tutkintopaiva",
    "tutkintokieli",
    "tutkintotaso",
    "jarjestajanTunnusOid",
    "jarjestajanNimi",
    "arviointipaiva",
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
    val suorittajanOppijanumero: Oid,
    @JsonProperty("hetu")
    val hetu: String,
    @JsonProperty("sukupuoli")
    val sukupuoli: Sukupuoli,
    @JsonProperty("sukunimi")
    val sukunimi: String,
    @JsonProperty("etunimet")
    val etunimet: String,
    @JsonProperty("kansalaisuus")
    val kansalaisuus: String,
    @JsonProperty("katuosoite")
    val katuosoite: String,
    @JsonProperty("postinumero")
    val postinumero: String,
    @JsonProperty("postitoimipaikka")
    val postitoimipaikka: String,
    @JsonProperty("email")
    val email: String?,
    @JsonProperty("tutkintopaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: Date,
    @JsonProperty("tutkintokieli")
    val tutkintokieli: Tutkintokieli,
    @JsonProperty("tutkintotaso")
    val tutkintotaso: Tutkintotaso,
    @JsonProperty("jarjestajanTunnusOid")
    val jarjestajanTunnusOid: Oid,
    @JsonProperty("jarjestajanNimi")
    val jarjestajanNimi: String,
    @JsonProperty("arviointipaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val arviointipaiva: Date,
    @JsonProperty("tekstinYmmartaminen")
    val tekstinYmmartaminen: Number?,
    @JsonProperty("kirjoittaminen")
    val kirjoittaminen: Number?,
    @JsonProperty("rakenteetJaSanasto")
    val rakenteetJaSanasto: Number?,
    @JsonProperty("puheenYmmartaminen")
    val puheenYmmartaminen: Number?,
    @JsonProperty("puhuminen")
    val puhuminen: Number?,
    @JsonProperty("yleisarvosana")
    val yleisarvosana: Number?,
) {
    fun toEntity(id: Number? = null) =
        YkiSuoritusEntity(
            id,
            suorittajanOppijanumero.toString(),
            hetu,
            sukupuoli,
            sukunimi,
            etunimet,
            kansalaisuus,
            katuosoite,
            postinumero,
            postitoimipaikka,
            email,
            tutkintopaiva,
            tutkintokieli,
            tutkintotaso,
            jarjestajanTunnusOid.toString(),
            jarjestajanNimi,
            arviointipaiva,
            tekstinYmmartaminen,
            kirjoittaminen,
            rakenteetJaSanasto,
            puheenYmmartaminen,
            puhuminen,
            yleisarvosana,
        )
}
