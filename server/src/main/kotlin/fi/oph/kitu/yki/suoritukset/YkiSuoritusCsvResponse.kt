package fi.oph.kitu.yki.suoritukset

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Arviointitila
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
    "maa",
    "email",
    "solkiTunniste",
    "lastModified",
    "tutkintopaiva",
    "tutkintokieli",
    "tutkintotaso",
    "todistuskieli",
    "jarjestajanOID",
    "jarjestajanNimi",
    "arviointitila",
    "tilaLahetetty",
    "arviointipaiva",
    "tekstinYmmartaminen",
    "kirjoittaminen",
    "puheenYmmartaminen",
    "puhuminen",
    "rakenteetJaSanasto",
    "yleisarvosana",
    "tarkistusarvioinninSaapumisPvm",
    "tarkistusarvioinninAsiatunnus",
    "tarkistusarvioidutOsakokeet",
    "arvosanaMuuttui",
    "perustelu",
    "tarkistusarvioinninKasittelyPvm",
)
data class YkiSuoritusCsvResponse(
    val suorittajanOID: Oid,
    val hetu: String?,
    val sukupuoli: Sukupuoli?,
    val sukunimi: String,
    val etunimet: String,
    val kansalaisuus: String,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val maa: String?,
    val email: String?,
    val solkiTunniste: Int,
    @param:JsonProperty("lastModified")
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val lastModified: Instant,
    @param:JsonProperty("tutkintopaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tutkintopaiva: LocalDate,
    @param:JsonProperty("tutkintokieli")
    @param:JsonDeserialize(using = TutkintokieliDeserializer::class)
    val tutkintokieli: Tutkintokieli,
    val tutkintotaso: Tutkintotaso,
    val todistuskieli: Todistuskieli?,
    val jarjestajanOID: Oid,
    val jarjestajanNimi: String,
    val arviointitila: Arviointitila,
    @param:JsonProperty("tilaLahetetty")
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val tilaLahetetty: Instant?,
    @param:JsonProperty("arviointipaiva")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val arviointipaiva: LocalDate?,
    val tekstinYmmartaminen: Int?,
    val kirjoittaminen: Int?,
    val rakenteetJaSanasto: Int?,
    val puheenYmmartaminen: Int?,
    val puhuminen: Int?,
    val yleisarvosana: Int?,
    @param:JsonProperty("tarkistusarvioinninSaapumisPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninSaapumisPvm: LocalDate?,
    val tarkistusarvioinninAsiatunnus: String?,
    val tarkistusarvioidutOsakokeet: String?,
    val arvosanaMuuttui: String?,
    val perustelu: String?,
    @param:JsonProperty("tarkistusarvioinninKasittelyPvm")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tarkistusarvioinninKasittelyPvm: LocalDate?,
)
