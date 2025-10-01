package fi.oph.kitu.vkt.html

import fi.oph.kitu.TypedResult
import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumerorekisteriHenkilo
import fi.oph.kitu.vkt.VktHenkilosuoritus
import fi.oph.kitu.vkt.VktOsakoe
import kotlinx.html.FlowContent
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3

object VktHyvaJaTyydyttavaTarkasteluPage {
    fun render(
        data: VktHenkilosuoritus,
        henkilo: TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>,
        translations: Translations,
        messages: List<ViewMessageData>,
        koskiTransferState: Pair<KoskiTransferState, List<String>>,
    ): String =
        Page.renderHtml {
            h1 { +data.henkilo.kokoNimi() }
            h2 { +"Valtionhallinnon kielitutkinto" }

            messages.forEach { viewMessage(it) }

            vktHenkilonTiedot(data, henkilo)
            vktSuorituksenTiedot(data, koskiTransferState, translations)

            h3 { +"Tutkinnot" }
            vktTutkinnot(data, translations)

            h3 { +"Osakokeet" }
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
            DisplayTableColumn("Osakoe", width = "20%") {
                +t.get(it.tyyppi)
            },
            DisplayTableColumn("Tutkintopäivä", width = "16%") {
                finnishDate(it.tutkintopaiva)
            },
            DisplayTableColumn("Arvosana", width = "16%") {
                it.arviointi?.arvosana?.let { arvosana -> +t.get(arvosana) }
            },
            DisplayTableColumn("Arviointipäivä", width = "16%") {
                it.arviointi?.paivamaara?.let { pvm -> finnishDate(pvm) }
            },
            DisplayTableColumn("Suorituksen vastaanottaja", width = "16%") {
                +it.suorituksenVastaanottaja.toString()
            },
            DisplayTableColumn("Suorituspaikkakunta", width = "16%") {
                +t.getByKoodiviite("kunta", it.suorituspaikkakunta)
            },
        ),
        testId = "osakokeet",
    )
}
