package fi.oph.kitu.vkt.html

import fi.oph.kitu.TypedResult
import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.vkt.VktOsakoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import kotlinx.html.FlowContent
import kotlinx.html.h1
import kotlinx.html.h2

object VktHyvaJaTyydyttavaTarkasteluPage {
    fun render(
        data: Henkilosuoritus<VktSuoritus>,
        henkilo: TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>,
        translations: Translations,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs(
                "/vkt/hyvajatyydyttava/suoritukset",
                Navigation.MenuItem(
                    data.henkilo.kokoNimi(),
                    "/vkt/suoritukset/${data.suoritus.internalId}",
                ),
            ),
        ) {
            h1 { +data.henkilo.kokoNimi() }

            vktHenkilonTiedot(data, henkilo)
            vktSuorituksenTiedot(data, translations)

            h2 { +"Tutkinnot" }
            vktTutkinnot(data, translations)

            h2 { +"Osakokeet" }
            card(overflowAuto = true) {
                vktHyvaJaTyydyttavaOsakoeTable(data.suoritus.osat, translations)
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
