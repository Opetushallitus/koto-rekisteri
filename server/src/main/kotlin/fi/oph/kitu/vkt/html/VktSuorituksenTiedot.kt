package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.FlowContent

fun FlowContent.vktSuorituksenTiedot(
    data: Henkilosuoritus<VktSuoritus>,
    t: Translations,
) {
    card(compact = true) {
        infoTable(
            "Tutkinnon taso" to { +t.get(data.suoritus.taitotaso) },
            "Kieli" to { +t.get(data.suoritus.kieli) },
            "Suorituspaikkakunta" to { +t.getByKoodiviite("kunta", data.suoritus.suorituspaikkakunta) },
        )
    }
}

fun FlowContent.vktTutkinnot(
    data: Henkilosuoritus<VktSuoritus>,
    t: Translations,
) {
    card(compact = true) {
        displayTable(
            rows = data.suoritus.tutkinnot,
            columns =
                listOf(
                    DisplayTableColumn("Tutkinto", width = "25%", testId = "tutkinto") {
                        +t.get(it.tyyppi)
                    },
                    DisplayTableColumn("Viimeisin tutkintopäivä", width = "25%", testId = "tutkintopaiva") {
                        it.viimeisinTutkintopaiva()?.let { finnishDate(it) }
                    },
                    DisplayTableColumn("Arvosana", width = "50%", testId = "arvosana") {
                        +t.get(it.arviointi()?.arvosana)
                    },
                ),
            testId = "tutkinnot",
            rowTestId = { it.tyyppi.koodiarvo },
        )
    }
}
