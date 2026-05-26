package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.Page
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.webmvc.Links
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

object YkiSuoritusPoikkeamaPage {
    fun render(poikkeamat: List<YkiSuoritusPoikkeama>): String =
        Page.renderHtml(wideContent = true) {
            h1 { +"Yleinen kielitutkinto" }
            h2 { +"Suoritusten poikkeamat" }

            if (poikkeamat.isEmpty()) {
                p { +"Ei havaittuja poikkeamia." }
            } else {
                article(classes = "overflow-auto") {
                    table(classes = "striped") {
                        thead {
                            tr {
                                th { +"Solki-ID" }
                                th { +"Kenttä" }
                                th { +"Arvo Kitussa" }
                                th { +"Arvo Solkissa" }
                                th { +"Havaittu" }
                            }
                        }
                        tbody {
                            poikkeamat.forEach { p ->
                                tr {
                                    td { a(href = Links.Yki.suoritus(p.solkiId)) { +p.solkiId.toString() } }
                                    td { +p.kentta }
                                    td { +p.arvoKitussa }
                                    td { +p.arvoSolkissa }
                                    td { finnishDateTime(p.havaittu) }
                                }
                            }
                        }
                    }
                }
            }
        }
}
