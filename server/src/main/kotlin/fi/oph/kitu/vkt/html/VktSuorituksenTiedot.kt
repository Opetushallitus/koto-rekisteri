package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.vkt.VktSuoritus
import kotlinx.html.FlowContent

fun FlowContent.vktSuorituksenTiedot(data: Henkilosuoritus<VktSuoritus>) {
    card {
        infoTable(
            "Tutkinnon taso" to { +data.suoritus.taitotaso.koodiarvo },
            "Kieli" to { +data.suoritus.kieli.koodiarvo },
            "Suorituspaikkakunta" to { +data.suoritus.suorituspaikkakunta.toString() },
        )
    }
}

fun FlowContent.vktTutkinnot(data: Henkilosuoritus<VktSuoritus>) {
    card {
        displayTable(
            rows = data.suoritus.tutkinnot,
            columns =
                listOf(
                    DisplayTableColumn("Tutkinto", width = "25%") {
                        +it.tyyppi.koodiarvo
                    },
                    DisplayTableColumn("Viimeisin tutkintopäivä", width = "25%") {
                        +(it.viimeisinTutkintopaiva()?.toString() ?: "")
                    },
                    DisplayTableColumn("Arvosana", width = "50%") {
                        +(it.arviointi()?.arvosana?.koodiarvo ?: "")
                    },
                ),
            compact = true,
        )
    }
}
