package fi.oph.kitu.yki

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste

data class YkiSuoritus(
    override val lahdejarjestelmanId: LahdejarjestelmanTunniste,
) : KielitutkinnonSuoritus {
    override val tyyppi: Koodisto.SuorituksenTyyppi = Koodisto.SuorituksenTyyppi.YleinenKielitutkinto
}
