package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Table(name = "yki_suoritus")
data class YkiSuoritusEntity(
    @Id
    val id: Int?,
    val suorittajanOID: Oid,
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
    val tutkintopaiva: LocalDate,
    @Enumerated(EnumType.STRING)
    val tutkintokieli: Tutkintokieli,
    @Enumerated(EnumType.STRING)
    val tutkintotaso: Tutkintotaso,
    val jarjestajanTunnusOid: Oid,
    val jarjestajanNimi: String,
    val arviointipaiva: LocalDate,
    val tekstinYmmartaminen: Int?,
    val kirjoittaminen: Int?,
    val rakenteetJaSanasto: Int?,
    val puheenYmmartaminen: Int?,
    val puhuminen: Int?,
    val yleisarvosana: Int?,
    val tarkistusarvioinninSaapumisPvm: LocalDate?,
    val tarkistusarvioinninAsiatunnus: String?,
    val tarkistusarvioidutOsakokeet: Set<TutkinnonOsa>?,
    val arvosanaMuuttui: Set<TutkinnonOsa>?,
    val perustelu: String?,
    val tarkistusarvioinninKasittelyPvm: LocalDate?,
    val koskiOpiskeluoikeus: Oid?,
    val koskiSiirtoKasitelty: Boolean?,
) {
    companion object
}
