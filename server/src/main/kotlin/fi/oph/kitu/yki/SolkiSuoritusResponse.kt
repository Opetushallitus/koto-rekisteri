package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import fi.oph.kitu.csvparsing.Features
import org.ietf.jgss.Oid
import java.time.Instant
import java.util.Date

@JsonPropertyOrder(
    "suorittajanOID",
    "hetu",
    "sukupuoli",
    "sukunimi",
    "etunimet",
    "kansalaisuus",
    "katuosoite",
    "postinumero",
    "postitoimipaikka",
    "email",
    "suoritusID",
    "lastModified",
    "tutkintopaiva",
    "tutkintokieli",
    "tutkintotaso",
    "jarjestajanOID",
    "jarjestajanNimi",
    "arviointipaiva",
    "tekstinYmmartaminen",
    "kirjoittaminen",
    "rakenteetJaSanasto",
    "puheenYmmartaminen",
    "puhuminen",
    "yleisarvosana",
    "tarkistusarvioinninSaapumisPvm",
    "tarkistusarvioinninAsiatunnus",
    "tarkistusarvioidutOsakokeet",
    "arvosanaMuuttui",
    "perustelu",
    "tarkistusarvioinninKasittelyPvm",
)
@Features(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
data class SolkiSuoritusResponse(
    @JsonProperty("suorittajanOID")
    val suorittajanOID: Oid,
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
    @JsonProperty("suoritusID")
    val suoritusID: Int,
    @JsonProperty("lastModified")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    val lastModified: Instant,
    @JsonProperty("tutkintopaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: Date,
    @JsonProperty("tutkintokieli")
    val tutkintokieli: Tutkintokieli,
    @JsonProperty("tutkintotaso")
    val tutkintotaso: Tutkintotaso,
    @JsonProperty("jarjestajanOID")
    val jarjestajanOID: Oid,
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
    @JsonProperty("tarkistusarvioinninSaapumisPvm")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninSaapumisPvm: Date?,
    @JsonProperty("tarkistusarvioinninAsiatunnus")
    val tarkistusarvioinninAsiatunnus: String?,
    @JsonProperty("tarkistusarvioidutOsakokeet")
    val tarkistusarvioidutOsakokeet: Number?,
    @JsonProperty("arvosanaMuuttui")
    val arvosanaMuuttui: Number?,
    @JsonProperty("perustelu")
    val perustelu: String?,
    @JsonProperty("tarkistusarvioinninKasittelyPvm")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninKasittelyPvm: Date?,
) {
    fun toEntity(id: Number? = null) =
        YkiSuoritusEntity(
            id,
            suorittajanOID.toString(),
            hetu,
            sukupuoli,
            sukunimi,
            etunimet,
            kansalaisuus,
            katuosoite,
            postinumero,
            postitoimipaikka,
            email,
            suoritusID,
            lastModified,
            tutkintopaiva,
            tutkintokieli,
            tutkintotaso,
            jarjestajanOID.toString(),
            jarjestajanNimi,
            arviointipaiva,
            tekstinYmmartaminen,
            kirjoittaminen,
            rakenteetJaSanasto,
            puheenYmmartaminen,
            puhuminen,
            yleisarvosana,
            tarkistusarvioinninSaapumisPvm,
            tarkistusarvioinninAsiatunnus,
            tarkistusarvioidutOsakokeet,
            arvosanaMuuttui == 1,
            perustelu,
            tarkistusarvioinninKasittelyPvm,
        )
}
