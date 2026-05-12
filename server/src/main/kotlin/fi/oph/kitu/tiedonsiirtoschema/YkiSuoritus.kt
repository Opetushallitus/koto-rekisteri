package fi.oph.kitu.tiedonsiirtoschema

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.TutkinnonOsa
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.Todistuskieli
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.LocalDate

typealias YkiHenkilosuoritus = Henkilosuoritus<YkiSuoritus>

data class YkiSuoritus(
    val tutkintotaso: Tutkintotaso,
    val kieli: Tutkintokieli,
    val todistuskieli: Todistuskieli?,
    val jarjestaja: YkiJarjestaja,
    val tutkintopaiva: LocalDate,
    val arviointipaiva: LocalDate?,
    val osat: List<YkiOsa>,
    val tarkistusarviointi: YkiTarkastusarviointi? = null,
    val arviointitila: Arviointitila,
    override val lahdejarjestelmanId: LahdejarjestelmanTunniste,
    override val internalId: Int? = null,
    override val koskiOpiskeluoikeusOid: Oid? = null,
    override val koskiSiirtoKasitelty: Boolean = false,
) : KielitutkinnonSuoritus {
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.YleinenKielitutkinto
}

data class YkiJarjestaja(
    val oid: Oid,
    val nimi: String,
)

data class YkiOsa(
    val tyyppi: TutkinnonOsa,
    val arvosana: Int?,
)

data class YkiTarkastusarviointi(
    val saapumispaiva: LocalDate,
    val kasittelypaiva: LocalDate?,
    val asiatunnus: String,
    val tarkistusarvioidutOsakokeet: List<TutkinnonOsa>?,
    val arvosanaMuuttui: List<TutkinnonOsa>?,
    val perustelu: String,
) {
    companion object {
        fun from(s: YkiSuoritusEntity): YkiTarkastusarviointi? =
            s.tarkistusarvioinninSaapumisPvm?.let {
                YkiTarkastusarviointi(
                    it,
                    s.tarkistusarvioinninKasittelyPvm,
                    s.tarkistusarvioinninAsiatunnus.orEmpty(),
                    s.tarkistusarvioidutOsakokeet?.toList(),
                    s.arvosanaMuuttui?.toList(),
                    s.perustelu.orEmpty(),
                )
            }
    }
}
