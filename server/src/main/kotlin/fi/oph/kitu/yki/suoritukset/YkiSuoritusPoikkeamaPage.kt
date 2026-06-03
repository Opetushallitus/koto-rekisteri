package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.CheckboxItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.checkboxDropdown
import fi.oph.kitu.html.csvDownloadButton
import fi.oph.kitu.html.javascript
import fi.oph.kitu.i18n.finnishDate
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
    fun render(
        poikkeamat: List<YkiSuoritusPoikkeama>,
        solkiIdToSuoritusId: Map<Int, Int>,
    ): String =
        Page.renderHtml(wideContent = true) {
            h1 { +"Yleinen kielitutkinto" }
            h2 { +"Suoritusten poikkeamat" }

            if (poikkeamat.isEmpty()) {
                p { +"Ei havaittuja poikkeamia." }
            } else {
                p { csvDownloadButton(Links.Yki.poikkeamatCsv()) }

                val kentat = poikkeamat.map { it.kentta }.distinct().sorted()

                article(classes = "overflow-auto") {
                    table(classes = "striped") {
                        thead {
                            tr {
                                th { +"Solki-ID" }
                                th { +"Tutkintopäivä" }
                                th { +"Kieli" }
                                th { +"Taso" }
                                th {
                                    +"Kenttä"
                                    checkboxDropdown(
                                        title = "Suodata",
                                        items =
                                            kentat.map {
                                                CheckboxItem(
                                                    value = it,
                                                    label = it,
                                                    testId = "kentta-filter-$it",
                                                )
                                            },
                                        classes = "kentta-filter",
                                        testId = "kentta-filter",
                                    )
                                }
                                th { +"Arvo Kitussa" }
                                th { +"Arvo Solkissa" }
                                th { +"Havaittu" }
                            }
                        }
                        tbody {
                            poikkeamat.forEach { p ->
                                tr {
                                    attributes["data-kentta"] = p.kentta
                                    td {
                                        val internalId = solkiIdToSuoritusId[p.solkiId]
                                        if (internalId != null) {
                                            a(href = Links.Yki.suoritus(internalId)) { +p.solkiId.toString() }
                                        } else {
                                            +p.solkiId.toString()
                                        }
                                    }
                                    td { p.tutkintopaiva?.let { finnishDate(it) } }
                                    td { p.tutkintokieli?.let { +it.name } }
                                    td { p.tutkintotaso?.let { +it.name } }
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
                    const dropdown = document.querySelector('.kentta-filter');
                    const checkboxes = dropdown.querySelectorAll('input[type=checkbox]');
                    const rows = document.querySelectorAll('tbody tr[data-kentta]');
                    dropdown.addEventListener('change', () => {
                        const active = new Set(
                            Array.from(checkboxes)
                                .filter(c => c.checked)
                                .map(c => c.value)
                        );
                        const filtering = active.size > 0;
                        rows.forEach(r => {
                            r.hidden = filtering && !active.has(r.dataset.kentta);
                        });
                    });
                    """.trimIndent(),
                )
            }
        }
}
