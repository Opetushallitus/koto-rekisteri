package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.Oid
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.TutkinnonOsa.Companion.toTutkinnonOsaSet
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.LocalDate

typealias YkiHenkilosuoritus = Henkilosuoritus<YkiSuoritus>

data class YkiSuoritus(
    val tutkintotaso: Tutkintotaso,
    val kieli: Tutkintokieli,
    val jarjestaja: YkiJarjestaja,
    val tutkintopaiva: LocalDate,
    val arviointipaiva: LocalDate?,
    val osat: List<YkiOsa>,
    val tarkistusarvointi: YkiTarkastusarvointi? = null,
    val arviointitila: Arviointitila,
    override val lahdejarjestelmanId: LahdejarjestelmanTunniste,
    override val internalId: Int? = null,
    override val koskiOpiskeluoikeusOid: Oid? = null,
    override val koskiSiirtoKasitelty: Boolean = false,
) : KielitutkinnonSuoritus {
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.YleinenKielitutkinto

    fun toYkiSuoritusEntity(henkilo: Henkilo): YkiSuoritusEntity {
        fun arvosana(tyyppi: TutkinnonOsa): Int? =
            osat
                .firstOrNull { it.tyyppi == tyyppi }
                ?.arvosana

        return YkiSuoritusEntity(
            id = internalId,
            suorittajanOID = henkilo.oid,
            hetu = henkilo.hetu ?: throw IllegalArgumentException("Hetu puuttuu"),
            sukupuoli = henkilo.sukupuoli ?: throw IllegalArgumentException("Sukupuoli puuttuu"),
            sukunimi = henkilo.sukunimi ?: throw IllegalArgumentException("Sukunimi puuttuu"),
            etunimet = henkilo.etunimet ?: throw IllegalArgumentException("Etunimet puuttuu"),
            kansalaisuus = henkilo.kansalaisuus ?: throw IllegalArgumentException("Kansalaisuus puuttuu"),
            katuosoite = henkilo.katuosoite ?: throw IllegalArgumentException("Katuosoite puuttuu"),
            postinumero = henkilo.postinumero ?: throw IllegalArgumentException("Postinumero puuttuu"),
            postitoimipaikka = henkilo.postitoimipaikka ?: throw IllegalArgumentException("Postitoimipaikka puuttuu"),
            email = henkilo.email,
            suoritusId = lahdejarjestelmanId.id.toInt(),
            lastModified = Instant.now(),
            tutkintopaiva = tutkintopaiva,
            tutkintokieli = kieli,
            tutkintotaso = tutkintotaso,
            jarjestajanTunnusOid = jarjestaja.oid,
            jarjestajanNimi = jarjestaja.nimi,
            arviointipaiva = arviointipaiva,
            tekstinYmmartaminen = arvosana(TutkinnonOsa.tekstinYmmartaminen),
            kirjoittaminen = arvosana(TutkinnonOsa.kirjoittaminen),
            rakenteetJaSanasto = arvosana(TutkinnonOsa.rakenteetJaSanasto),
            puheenYmmartaminen = arvosana(TutkinnonOsa.puheenYmmartaminen),
            puhuminen = arvosana(TutkinnonOsa.puhuminen),
            yleisarvosana = arvosana(TutkinnonOsa.yleisarvosana),
            tarkistusarvioinninSaapumisPvm = tarkistusarvointi?.saapumispaiva,
            tarkistusarvioinninAsiatunnus = tarkistusarvointi?.asiatunnus,
            tarkistusarvioidutOsakokeet =
                tarkistusarvointi?.tarkistusarvioidutOsakokeet?.toTutkinnonOsaSet(),
            arvosanaMuuttui =
                tarkistusarvointi?.arvosanaMuuttui?.toTutkinnonOsaSet(),
            perustelu = tarkistusarvointi?.perustelu,
            tarkistusarvioinninKasittelyPvm = tarkistusarvointi?.kasittelypaiva,
            koskiOpiskeluoikeus = koskiOpiskeluoikeusOid,
            koskiSiirtoKasitelty = koskiSiirtoKasitelty,
            arviointitila = arviointitila,
        )
    }
}

data class YkiJarjestaja(
    val oid: Oid,
    val nimi: String,
)

data class YkiOsa(
    val tyyppi: TutkinnonOsa,
    val arvosana: Int?,
)

data class YkiTarkastusarvointi(
    val saapumispaiva: LocalDate,
    val kasittelypaiva: LocalDate?,
    val asiatunnus: String,
    val tarkistusarvioidutOsakokeet: Int?,
    val arvosanaMuuttui: Int?,
    val perustelu: String,
)
