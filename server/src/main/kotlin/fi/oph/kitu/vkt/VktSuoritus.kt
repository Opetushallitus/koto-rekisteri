package fi.oph.kitu.vkt

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import fi.oph.kitu.tiedonsiirtoschema.Osasuorituksellinen
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

typealias VktHenkilosuoritus = Henkilosuoritus<VktSuoritus>

data class VktSuoritus(
    val taitotaso: Koodisto.VktTaitotaso,
    val kieli: Koodisto.Tutkintokieli,
    val suorituksenVastaanottaja: Oid? = null, // henkilö-oid
    val suorituspaikkakunta: String? = null, // kunta-koodiston mukainen koodiarvo
    @get:JsonProperty("osakokeet")
    override val osat: List<VktOsakoe>,
    override val lahdejarjestelmanId: LahdejarjestelmanTunniste,
    override val internalId: Int? = null,
    override val koskiOpiskeluoikeusOid: Oid? = null,
    override val koskiSiirtoKasitelty: Boolean = false,
    @field:Schema(hidden = true) val merkittyPoistettavaksi: Boolean = false,
) : KielitutkinnonSuoritus,
    Osasuorituksellinen {
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.ValtionhallinnonKielitutkinto

    @get:JsonIgnore
    val tutkinnot: List<VktTutkinto> by lazy {
        listOf(
            VktKirjallinenKielitaito.from(osat),
            VktSuullinenKielitaito.from(osat),
            VktYmmartamisenKielitaito.from(osat),
        ).flatten().sortedWith(compareByDescending(VktTutkinto::tutkintopaivaTodistuksella).thenBy { it.tyyppi.name })
    }

    @get:JsonIgnore
    val tutkintopaiva: LocalDate? by lazy {
        osat.maxOfOrNull { it.tutkintopaiva }
    }

    fun toVktSuoritusEntity(oppija: Henkilo): VktSuoritusEntity =
        VktSuoritusEntity(
            ilmoittautumisenId = lahdejarjestelmanId.toString(),
            suorittajanOid = oppija.oid,
            etunimet = oppija.etunimet.orEmpty(),
            sukunimi = oppija.sukunimi.orEmpty(),
            tutkintokieli = kieli,
            suorituspaikkakunta = suorituspaikkakunta,
            taitotaso = taitotaso,
            suorituksenVastaanottaja = suorituksenVastaanottaja,
            osakokeet = osat.map { it.toVktOsakoeRow() }.toSet(),
            tutkinnot = tutkinnot.map { it.toVktTutkintoRow() }.toSet(),
            koskiOpiskeluoikeus = koskiOpiskeluoikeusOid,
            koskiSiirtoKasitelty = koskiSiirtoKasitelty,
        )
}
