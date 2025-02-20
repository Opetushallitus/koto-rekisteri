package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

@Suppress("ktlint:standard:argument-list-wrapping")
val ykiSuoritusEntityColumns =
    listOf(
        "id", "suorittajan_oid", "sukunimi", "etunimet", "tutkintopaiva", "tutkintokieli", "tutkintotaso",
        "jarjestajan_tunnus_oid", "jarjestajan_nimi", "tekstin_ymmartaminen", "kirjoittaminen", "rakenteet_ja_sanasto",
        "puheen_ymmartaminen", "puhuminen", "yleisarvosana", "hetu", "sukupuoli", "kansalaisuus", "katuosoite",
        "postinumero", "postitoimipaikka", "email", "arviointipaiva", "suoritus_id", "last_modified",
        "tarkistusarvioinnin_saapumis_pvm", "tarkistusarvioinnin_asiatunnus", "tarkistusarvioidut_osakokeet",
        "arvosana_muuttui", "perustelu", "tarkistusarvioinnin_kasittely_pvm",
    )

@Table(name = "yki_suoritus")
data class YkiSuoritusEntity(
    @Id
    val id: Int?,
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
    val tutkintopaiva: LocalDate,
    @Enumerated(EnumType.STRING)
    val tutkintokieli: Tutkintokieli,
    @Enumerated(EnumType.STRING)
    val tutkintotaso: Tutkintotaso,
    val jarjestajanTunnusOid: String,
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
    /*
     * kokonaisluku 0–15, tulkitaan bittimaskina
     * 1=puhuminen
     * 2=kirjoittaminen
     * 4=tekstin ymmärtäminen
     * 8=puheen ymmärtäminen
     * */
    val tarkistusarvioidutOsakokeet: Int?,
    /*
     * kokonaisluku 0–15, tulkitaan bittimaskina
     * 1=puhuminen
     * 2=kirjoittaminen
     * 4=tekstin ymmärtäminen
     * 8=puheen ymmärtäminen
     * */
    val arvosanaMuuttui: Int?,
    val perustelu: String?,
    val tarkistusarvioinninKasittelyPvm: LocalDate?,
) {
    companion object
}
