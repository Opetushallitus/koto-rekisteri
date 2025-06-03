package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.MenuItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.submitButton
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.FlowContent
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2

object VktHyvaJaTyydyttavaTarkastelu {
    fun render(
        data: Henkilosuoritus<VktSuoritus>,
        translations: Translations,
    ): String =
        Page.renderHtml(
            listOf(
                MenuItem("Valtionhallinnon kielitutkinto", "/vkt/ilmoittautuneet"),
                MenuItem("Ilmoittautuneet", "/vkt/ilmoittautuneet"),
                MenuItem(data.henkilo.kokoNimi(), "/vkt/ilmoittautuneet/${data.suoritus.internalId}"),
            ),
        ) {
            h1 { +data.henkilo.kokoNimi() }

            vktSuorituksenTiedot(data, translations)

            h2 { +"Tutkinnot" }
            vktTutkinnot(data, translations)

            h2 { +"Osakokeet" }
            card(overflowAuto = true) {
                vktHyvaJaTyydyttavaOsakoeTable(data.suoritus.osat, translations)
                footer {
                    submitButton()
                }
            }
        }
}

fun FlowContent.vktHyvaJaTyydyttavaOsakoeTable(
    osat: List<VktOsakoe>,
    t: Translations,
) {
    displayTable(
        osat.sortedBy { it.tutkintopaiva }.reversed(),
        listOf(
            DisplayTableColumn("Osakoe", width = "25%") {
                +t.get(it.tyyppi)
            },
            DisplayTableColumn("Tutkintopäivä", width = "25%") {
                finnishDate(it.tutkintopaiva)
            },
            DisplayTableColumn("Arvosana", width = "25%") {
                it.arviointi?.arvosana?.let { arvosana -> +t.get(arvosana) }
            },
            DisplayTableColumn("Arviointipäivä", width = "25%") {
                it.arviointi?.paivamaara?.let { pvm -> finnishDate(pvm) }
            },
        ),
        compact = true,
        testId = "osakokeet",
    )
}
