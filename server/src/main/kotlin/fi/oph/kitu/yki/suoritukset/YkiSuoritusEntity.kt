package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.Date

@Table(name = "yki_suoritus")
data class YkiSuoritusEntity(
    @Id
    val id: Number?,
    val suorittajanOID: String,
    val hetu: String,
    @Enumerated(EnumType.STRING)
    val sukupuoli: Sukupuoli,
    val sukunimi: String,
    val etunimet: String,
    val kansalaisuus: String,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val email: String?,
    val suoritusId: Int,
    val lastModified: Instant,
    val tutkintopaiva: Date,
    @Enumerated(EnumType.STRING)
    val tutkintokieli: Tutkintokieli,
    @Enumerated(EnumType.STRING)
    val tutkintotaso: Tutkintotaso,
    val jarjestajanTunnusOid: String,
    val jarjestajanNimi: String,
    val arviointipaiva: Date,
    val tekstinYmmartaminen: Number?,
    val kirjoittaminen: Number?,
    val rakenteetJaSanasto: Number?,
    val puheenYmmartaminen: Number?,
    val puhuminen: Number?,
    val yleisarvosana: Number?,
    val tarkistusarvioinninSaapumisPvm: Date?,
    val tarkistusarvioinninAsiatunnus: String?,
    val tarkistusarvioidutOsakokeet: Number?,
    val arvosanaMuuttui: Boolean?,
    val perustelu: String?,
    val tarkistusarvioinninKasittelyPvm: Date?,
)
