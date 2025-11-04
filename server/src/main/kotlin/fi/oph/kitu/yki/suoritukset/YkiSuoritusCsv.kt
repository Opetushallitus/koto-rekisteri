package fi.oph.kitu.yki.suoritukset

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import fi.oph.kitu.Oid
import fi.oph.kitu.csvparsing.Features
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.arvioijat.TutkintokieliDeserializer
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
    @param:JsonProperty("suorittajanOID")
    val suorittajanOID: Oid,
    @param:JsonProperty("hetu")
    val hetu: String,
    @param:JsonProperty("sukupuoli")
    val sukupuoli: Sukupuoli?,
    @param:JsonProperty("sukunimi")
    val sukunimi: String,
    @param:JsonProperty("etunimet")
    val etunimet: String,
    @param:JsonProperty("kansalaisuus")
    val kansalaisuus: String,
    @param:JsonProperty("katuosoite")
    val katuosoite: String,
    @param:JsonProperty("postinumero")
    val postinumero: String,
    @param:JsonProperty("postitoimipaikka")
    val postitoimipaikka: String,
    @param:JsonProperty("email")
    val email: String?,
    @param:JsonProperty("suoritusID")
    val suoritusID: Int,
    @param:JsonProperty("lastModified")
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val lastModified: Instant,
    @param:JsonProperty("tutkintopaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: LocalDate,
    @param:JsonProperty("tutkintokieli")
    @param:JsonDeserialize(using = TutkintokieliDeserializer::class)
    val tutkintokieli: Tutkintokieli,
    @param:JsonProperty("tutkintotaso")
    val tutkintotaso: Tutkintotaso,
    @param:JsonProperty("jarjestajanOID")
    val jarjestajanOID: Oid,
    @param:JsonProperty("jarjestajanNimi")
    val jarjestajanNimi: String,
    @param:JsonProperty("arviointipaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val arviointipaiva: LocalDate,
    @param:JsonProperty("tekstinYmmartaminen")
    val tekstinYmmartaminen: Int?,
    @param:JsonProperty("kirjoittaminen")
    val kirjoittaminen: Int?,
    @param:JsonProperty("rakenteetJaSanasto")
    val rakenteetJaSanasto: Int?,
    @param:JsonProperty("puheenYmmartaminen")
    val puheenYmmartaminen: Int?,
    @param:JsonProperty("puhuminen")
    val puhuminen: Int?,
    @param:JsonProperty("yleisarvosana")
    val yleisarvosana: Int?,
    @param:JsonProperty("tarkistusarvioinninSaapumisPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninSaapumisPvm: LocalDate?,
    @param:JsonProperty("tarkistusarvioinninAsiatunnus")
    val tarkistusarvioinninAsiatunnus: String?,
    @param:JsonProperty("tarkistusarvioidutOsakokeet")
    val tarkistusarvioidutOsakokeet: String?,
    @param:JsonProperty("arvosanaMuuttui")
    val arvosanaMuuttui: String?,
    @param:JsonProperty("perustelu")
    val perustelu: String?,
    @param:JsonProperty("tarkistusarvioinninKasittelyPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninKasittelyPvm: LocalDate?,
)
