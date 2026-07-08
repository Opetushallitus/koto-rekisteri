package fi.oph.kitu.kotoutumiskoulutus.suoritukset
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.organisaatiot.Organisaatiot
import kotlinx.html.FlowContent
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3

object KielitestiSuoritusPage {
    fun render(
        suoritus: KielitestiSuoritus,
        orgs: Organisaatiot,
    ) = Page.renderHtml {
        h1 { +"${suoritus.sukunimi} ${suoritus.etunimet}" }
        h2 { +UiText.Nav.kotoutumiskoulutuksenPaattotesti }
        henkilonTiedot(suoritus)
        tutkintotiedot(suoritus, orgs)
        arviointi(suoritus)
        integraatiot(suoritus)
    }

    fun FlowContent.henkilonTiedot(suoritus: KielitestiSuoritus) {
        h3 { +UiText.Koto.henkilotiedot }
        card(compact = true) {
            infoTable(
                UiText.Koto.Sarake.oppijanumero
                    .toString() to { +(suoritus.oppijanumero?.toString() ?: "-") },
                UiText.Koto.Sarake.sukunimi
                    .toString() to { +suoritus.sukunimi },
                UiText.Koto.Sarake.etunimet
                    .toString() to { +suoritus.etunimet },
                UiText.Koto.Sarake.kutsumanimi
                    .toString() to { +suoritus.kutsumanimi },
                UiText.Koto.Sarake.sahkoposti
                    .toString() to { +suoritus.email },
            )
        }
    }

    fun FlowContent.tutkintotiedot(
        suoritus: KielitestiSuoritus,
        orgs: Organisaatiot,
    ) {
        h3 { +UiText.Koto.tutkinnonTiedot }
        card(compact = true) {
            infoTable(
                UiText.Koto.kurssi.toString() to { +"${suoritus.kurssi} (${suoritus.kurssiId})" },
                UiText.Koto.jarjestaja.toString() to
                    { +"${orgs.nimet[suoritus.oppilaitosOid]} (${suoritus.oppilaitosOid})" },
                UiText.Koto.Sarake.opettajanSahkopostiosoite
                    .toString() to { +suoritus.opettajanEmail.orEmpty() },
                UiText.Koto.Sarake.suoritusaika
                    .toString() to
                    { suoritus.suoritusaika?.let { finnishDateTime(it) } ?: +"-" },
                UiText.Koto.Sarake.testikieli
                    .toString() to { +(suoritus.testikieli?.toString() ?: "-") },
                UiText.Koto.tehtavapaketti.toString() to { +suoritus.tehtavapaketti.orEmpty() },
            )
        }
    }

    fun FlowContent.arviointi(suoritus: KielitestiSuoritus) {
        h3 { +UiText.Koto.arviointi }
        card(compact = true) {
            infoTable(
                UiText.Koto.Sarake.luetunYmmartaminen
                    .toString() to
                    { +(suoritus.luetunYmmartaminen?.toString() ?: "-") },
                UiText.Koto.Sarake.kuullunYmmartaminen
                    .toString() to
                    { +(suoritus.kuullunYmmartaminen?.toString() ?: "-") },
                UiText.Koto.Sarake.puhe
                    .toString() to { +(suoritus.puhe?.toString() ?: "-") },
                UiText.Koto.Sarake.kirjoittaminen
                    .toString() to { +(suoritus.kirjoittaminen?.toString() ?: "-") },
            )
        }
    }

    fun FlowContent.integraatiot(suoritus: KielitestiSuoritus) {
        h3 { +UiText.Koto.integraatiot }
        card(compact = true) {
            infoTable(
                UiText.Koto.viimeksiMuokattu.toString() to { finnishDateTime(suoritus.lastModified) },
            )
        }
    }
}
