package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.javascript
import fi.oph.kitu.html.testId
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.webmvc.Links
import kotlinx.html.ButtonType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.button
import kotlinx.html.div
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
                val kentat = poikkeamat.map { it.kentta }.distinct().sorted()

                div(classes = "kentta-filters") {
                    attributes["role"] = "group"
                    kentat.forEach { kentta ->
                        button(type = ButtonType.button, classes = "kentta-filter") {
                            attributes["aria-pressed"] = "true"
                            attributes["data-kentta-filter"] = kentta
                            testId("kentta-filter-$kentta")
                            +kentta
                        }
                    }
                }

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
                                    attributes["data-kentta"] = p.kentta
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

                javascript(
                    """
                    const buttons = document.querySelectorAll('.kentta-filter');
                    const rows = document.querySelectorAll('tbody tr[data-kentta]');
                    function apply() {
                        const active = new Set(
                            Array.from(buttons)
                                .filter(b => b.getAttribute('aria-pressed') === 'true')
                                .map(b => b.dataset.kenttaFilter)
                        );
                        rows.forEach(r => {
                            r.hidden = !active.has(r.dataset.kentta);
                        });
                    }
                    buttons.forEach(b => b.addEventListener('click', () => {
                        const pressed = b.getAttribute('aria-pressed') === 'true';
                        b.setAttribute('aria-pressed', String(!pressed));
                        apply();
                    }));
                    """.trimIndent(),
                )
            }
        }
}
