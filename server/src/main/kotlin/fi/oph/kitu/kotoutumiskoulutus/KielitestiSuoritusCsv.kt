package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MapperFeature
import fi.oph.kitu.Oid
import fi.oph.kitu.csvparsing.Features
import java.time.Instant

@JsonPropertyOrder(
    "sukunimi",
    "etunimet",
    "sahkoposti",
    "kurssinNimi",
    "suoritusaika",
    "oppijanumero",
    "luetunYmmartaminen",
    "kuullunYmmartaminen",
    "puhuminen",
    "kirjoittaminen",
)
@Features(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
data class KielitestiSuoritusCsv(
    val sukunimi: String,
    val etunimet: String,
    val sahkoposti: String,
    val kurssinNimi: String,
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
        fun of(s: KielitestiSuoritus): KielitestiSuoritusCsv =
            KielitestiSuoritusCsv(
                sukunimi = s.lastName,
                etunimet = s.firstNames,
                sahkoposti = s.email,
                kurssinNimi = s.coursename,
                suoritusaika = s.timeCompleted,
                oppijanumero = s.oppijanumero,
                luetunYmmartaminen = s.luetunYmmartaminenResult,
                kuullunYmmartaminen = s.kuullunYmmartaminenResult,
                puhuminen = s.puheResult,
                kirjoittaminen = s.kirjoittaminenResult,
            )
    }
}
