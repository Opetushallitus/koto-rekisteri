package fi.oph.kitu.yki.suoritukset

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import fi.oph.kitu.csvparsing.BooleanFromNumericDeserializer
import fi.oph.kitu.csvparsing.Features
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.ietf.jgss.Oid
import java.time.Instant
import java.time.LocalDate

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
data class YkiSuoritusCsv(
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val lastModified: Instant,
    @JsonProperty("tutkintopaiva")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: LocalDate,
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
    val arviointipaiva: LocalDate,
    @JsonProperty("tekstinYmmartaminen")
    val tekstinYmmartaminen: Double?,
    @JsonProperty("kirjoittaminen")
    val kirjoittaminen: Double?,
    @JsonProperty("rakenteetJaSanasto")
    val rakenteetJaSanasto: Double?,
    @JsonProperty("puheenYmmartaminen")
    val puheenYmmartaminen: Double?,
    @JsonProperty("puhuminen")
    val puhuminen: Double?,
    @JsonProperty("yleisarvosana")
    val yleisarvosana: Double?,
    @JsonProperty("tarkistusarvioinninSaapumisPvm")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninSaapumisPvm: LocalDate?,
    @JsonProperty("tarkistusarvioinninAsiatunnus")
    val tarkistusarvioinninAsiatunnus: String?,
    @JsonProperty("tarkistusarvioidutOsakokeet")
    val tarkistusarvioidutOsakokeet: Int?,
    @JsonProperty("arvosanaMuuttui")
    @JsonDeserialize(using = BooleanFromNumericDeserializer::class)
    val arvosanaMuuttui: Boolean?,
    @JsonProperty("perustelu")
    val perustelu: String?,
    @JsonProperty("tarkistusarvioinninKasittelyPvm")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninKasittelyPvm: LocalDate?,
) {
    fun toEntity(id: Int? = null) =
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
            arvosanaMuuttui,
            perustelu,
            tarkistusarvioinninKasittelyPvm,
        )
}
