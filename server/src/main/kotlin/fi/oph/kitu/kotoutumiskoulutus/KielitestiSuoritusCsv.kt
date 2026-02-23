package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import fi.oph.kitu.Oid
import fi.oph.kitu.csvparsing.Features
import fi.oph.kitu.organisaatiot.Organisaatiot
import java.time.Instant

@JsonPropertyOrder(
    "oppijanumero",
    "sukunimi",
    "etunimet",
    "kutsumanimi",
    "sahkoposti",
    "kurssiId",
    "kurssinNimi",
    "organisaatioOid",
    "organisaatio",
    "suoritusaika",
    "luetunYmmartaminen",
    "kuullunYmmartaminen",
    "puhuminen",
    "kirjoittaminen",
)
@Features(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
data class KielitestiSuoritusCsv(
    val sukunimi: String,
    val etunimet: String,
    val kutsumanimi: String,
    val sahkoposti: String,
    val kurssinNimi: String,
    val kurssiId: Int,
    val organisaatioOid: Oid?,
    val organisaatio: String?,
    @param:JsonProperty("suoritusaika")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    val suoritusaika: Instant,
    val oppijanumero: Oid,
    val luetunYmmartaminen: String,
    val kuullunYmmartaminen: String,
    val puhuminen: String,
    val kirjoittaminen: String?,
) {
    companion object {
        fun of(
            s: KielitestiSuoritus,
            organisaatiot: Organisaatiot,
        ): KielitestiSuoritusCsv =
            KielitestiSuoritusCsv(
                sukunimi = s.sukunimi,
                etunimet = s.etunimet,
                kutsumanimi = s.kutsumanimi,
                sahkoposti = s.email,
                kurssiId = s.kurssiId,
                kurssinNimi = s.kurssi,
                organisaatioOid = s.oppilaitosOid,
                organisaatio = organisaatiot.nimet[s.oppilaitosOid]?.toString(),
                suoritusaika = s.suoritusaika,
                oppijanumero = s.oppijanumero,
                luetunYmmartaminen = s.luetunYmmartaminen,
                kuullunYmmartaminen = s.kuullunYmmartaminen,
                puhuminen = s.puhe,
                kirjoittaminen = s.kirjoittaminen,
            )
    }
}
