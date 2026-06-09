package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.infoTable
import fi.oph.kitu.i18n.finnishDateTime
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
        h2 { +"Kotoutumiskoulutuksen kielitaidon päättötesti" }
        henkilonTiedot(suoritus)
        tutkintotiedot(suoritus, orgs)
        arviointi(suoritus)
        integraatiot(suoritus)
    }

    fun FlowContent.henkilonTiedot(suoritus: KielitestiSuoritus) {
        h3 { +"Henkilötiedot" }
        card(compact = true) {
            infoTable(
                "Oppijanumero" to { +(suoritus.oppijanumero?.toString() ?: "-") },
                "Sukunimi" to { +suoritus.sukunimi },
                "Etunimet" to { +suoritus.etunimet },
                "Kutsumanimi" to { +suoritus.kutsumanimi },
                "Sähköposti" to { +suoritus.email },
            )
        }
    }

    fun FlowContent.tutkintotiedot(
        suoritus: KielitestiSuoritus,
        orgs: Organisaatiot,
    ) {
        h3 { +"Tutkinnon tiedot" }
        card(compact = true) {
            infoTable(
                "Kurssi" to { +"${suoritus.kurssi} (${suoritus.kurssiId})" },
                "Järjestäjä" to { +"${orgs.nimet[suoritus.oppilaitosOid]} (${suoritus.oppilaitosOid})" },
                "Opettajan sähköpostiosoite" to { +suoritus.opettajanEmail.orEmpty() },
                "Suoritusaika" to { suoritus.suoritusaika?.let { finnishDateTime(it) } ?: +"-" },
                "Testikieli" to { +(suoritus.testikieli?.toString() ?: "-") },
                "Tehtäväpaketti" to { +suoritus.tehtavapaketti.orEmpty() },
            )
        }
    }

    fun FlowContent.arviointi(suoritus: KielitestiSuoritus) {
        h3 { +"Arvionti" }
        card(compact = true) {
            infoTable(
                "Luetun ymmärtäminen" to { +(suoritus.luetunYmmartaminen?.toString() ?: "-") },
                "Kuullun ymmärtäminen" to { +(suoritus.kuullunYmmartaminen?.toString() ?: "-") },
                "Puhe" to { +(suoritus.puhe?.toString() ?: "-") },
                "Kirjoittaminen" to { +(suoritus.kirjoittaminen?.toString() ?: "-") },
            )
        }
    }

    fun FlowContent.integraatiot(suoritus: KielitestiSuoritus) {
        h3 { +"Integraatiot" }
        card(compact = true) {
            infoTable(
                "Viimeksi muokattu" to { finnishDateTime(suoritus.lastModified) },
            )
        }
    }
}
