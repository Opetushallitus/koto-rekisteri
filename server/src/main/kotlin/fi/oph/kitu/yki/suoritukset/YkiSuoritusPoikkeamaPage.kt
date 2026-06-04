package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.CheckboxItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.checkboxDropdown
import fi.oph.kitu.html.csvDownloadButton
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.input
import fi.oph.kitu.html.javascript
import fi.oph.kitu.html.testId
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.webmvc.Links
import kotlinx.html.ButtonType
import kotlinx.html.InputType
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
    fun render(
        poikkeamat: List<YkiSuoritusPoikkeama>,
        solkiIdToSuoritusId: Map<Int, Int>,
        message: ViewMessageData? = null,
    ): String =
        Page.renderHtml(wideContent = true) {
            h1 { +"Yleinen kielitutkinto" }
            h2 { +"Suoritusten poikkeamat" }

            viewMessage(message)

            if (poikkeamat.isEmpty()) {
                p { +"Ei havaittuja poikkeamia." }
            } else {
                val kentat = poikkeamat.map { it.kentta }.distinct().sorted()
                val tutkintopaivat = poikkeamat.mapNotNull { it.tutkintopaiva }.distinct().sortedDescending()
                val tutkintokielet = poikkeamat.mapNotNull { it.tutkintokieli }.distinct().sortedBy { it.name }
                val tutkintotasot = poikkeamat.mapNotNull { it.tutkintotaso }.distinct().sortedBy { it.name }

                formPost(action = Links.Yki.poikkeamatPatch()) {
                    div(classes = "poikkeamat-toolbar") {
                        csvDownloadButton(Links.Yki.poikkeamatCsv())
                        button(type = ButtonType.submit, classes = "patch-button") {
                            attributes["disabled"] = ""
                            attributes["data-patch-button"] = ""
                            testId("tallenna-korjaukset")
                            +"Tallenna korjaukset"
                        }
                    }

                    article(classes = "overflow-auto") {
                        table(classes = "striped") {
                            thead {
                                tr {
                                    th { +"Solki-ID" }
                                    th {
                                        +"Tutkintopäivä"
                                        checkboxDropdown(
                                            title = "Suodata",
                                            items =
                                                tutkintopaivat.map {
                                                    CheckboxItem(
                                                        value = it.toString(),
                                                        label = it.finnishDate(),
                                                        testId = "tutkintopaiva-filter-$it",
                                                    )
                                                },
                                            testId = "tutkintopaiva-filter",
                                            dataAttributes = mapOf("filter-key" to "tutkintopaiva"),
                                        )
                                    }
                                    th {
                                        +"Kieli"
                                        checkboxDropdown(
                                            title = "Suodata",
                                            items =
                                                tutkintokielet.map {
                                                    CheckboxItem(
                                                        value = it.name,
                                                        label = it.name,
                                                        testId = "tutkintokieli-filter-${it.name}",
                                                    )
                                                },
                                            testId = "tutkintokieli-filter",
                                            dataAttributes = mapOf("filter-key" to "tutkintokieli"),
                                        )
                                    }
                                    th {
                                        +"Taso"
                                        checkboxDropdown(
                                            title = "Suodata",
                                            items =
                                                tutkintotasot.map {
                                                    CheckboxItem(
                                                        value = it.name,
                                                        label = it.name,
                                                        testId = "tutkintotaso-filter-${it.name}",
                                                    )
                                                },
                                            testId = "tutkintotaso-filter",
                                            dataAttributes = mapOf("filter-key" to "tutkintotaso"),
                                        )
                                    }
                                    th {
                                        input(type = InputType.checkBox) {
                                            attributes["data-select-all-visible"] = ""
                                            testId("valitse-nakyvat")
                                        }
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
                                            testId = "kentta-filter",
                                            dataAttributes = mapOf("filter-key" to "kentta"),
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
                                        attributes["data-solki-id"] = p.solkiId.toString()
                                        attributes["data-kentta"] = p.kentta
                                        p.tutkintopaiva?.let { attributes["data-tutkintopaiva"] = it.toString() }
                                        p.tutkintokieli?.let { attributes["data-tutkintokieli"] = it.name }
                                        p.tutkintotaso?.let { attributes["data-tutkintotaso"] = it.name }
                                        td(classes = "group-cell") {
                                            div(classes = "select-group") {
                                                input(type = InputType.checkBox) {
                                                    attributes["data-select-group"] = p.solkiId.toString()
                                                    testId("select-group-${p.solkiId}")
                                                }
                                                val internalId = solkiIdToSuoritusId[p.solkiId]
                                                if (internalId != null) {
                                                    a(href = Links.Yki.suoritus(internalId)) { +p.solkiId.toString() }
                                                } else {
                                                    +p.solkiId.toString()
                                                }
                                            }
                                        }
                                        td(classes = "group-cell") { p.tutkintopaiva?.let { finnishDate(it) } }
                                        td(classes = "group-cell") { p.tutkintokieli?.let { +it.name } }
                                        td(classes = "group-cell") { p.tutkintotaso?.let { +it.name } }
                                        td {
                                            div(classes = "select-group") {
                                                if (p.kentta != YkiSuoritusPoikkeama.SUORITUS_PUUTTUU_KITUSTA) {
                                                    input(
                                                        type = InputType.checkBox,
                                                        name = "poikkeama",
                                                        value = PoikkeamaKey(p.solkiId, p.kentta).encode(),
                                                    ) {
                                                        attributes["data-poikkeama-checkbox"] = ""
                                                        testId("poikkeama-checkbox-${p.solkiId}-${p.kentta}")
                                                    }
                                                }
                                                +p.kentta
                                            }
                                        }
                                        td { +p.arvoKitussa }
                                        td { +p.arvoSolkissa }
                                        td { finnishDateTime(p.havaittu) }
                                    }
                                }
                            }
                        }
                    }
                }

                javascript(
                    """
                    const dropdowns = document.querySelectorAll('thead [data-filter-key]');
                    const rows = document.querySelectorAll('tbody tr');
                    const patchButton = document.querySelector('[data-patch-button]');
                    const selectAllVisible = document.querySelector('[data-select-all-visible]');
                    const patchCheckboxes = document.querySelectorAll('[data-poikkeama-checkbox]');

                    function apply() {
                        rows.forEach(r => {
                            let visible = true;
                            for (const d of dropdowns) {
                                const active = Array.from(
                                    d.querySelectorAll('input[type=checkbox]:checked')
                                ).map(c => c.value);
                                if (active.length > 0 && !active.includes(r.getAttribute('data-' + d.dataset.filterKey))) {
                                    visible = false;
                                    break;
                                }
                            }
                            r.hidden = !visible;
                        });
                        let prevSolkiId = null;
                        rows.forEach(r => {
                            if (r.hidden) return;
                            const id = r.dataset.solkiId;
                            r.classList.toggle('repeat-group', id === prevSolkiId);
                            prevSolkiId = id;
                        });
                        if (patchButton) {
                            patchButton.disabled = !document.querySelector('[data-poikkeama-checkbox]:checked');
                        }
                        document.querySelectorAll('[data-select-group]').forEach(groupCb => {
                            const groupId = groupCb.dataset.selectGroup;
                            const rowCbs = document.querySelectorAll(
                                'tr[data-solki-id="' + groupId + '"] [data-poikkeama-checkbox]'
                            );
                            const checkedCount = Array.from(rowCbs).filter(c => c.checked).length;
                            groupCb.checked = checkedCount > 0 && checkedCount === rowCbs.length;
                            groupCb.indeterminate = checkedCount > 0 && checkedCount < rowCbs.length;
                        });
                    }

                    dropdowns.forEach(d => d.addEventListener('change', apply));
                    patchCheckboxes.forEach(cb => cb.addEventListener('change', apply));

                    if (selectAllVisible) {
                        selectAllVisible.addEventListener('change', () => {
                            rows.forEach(r => {
                                if (r.hidden) return;
                                const cb = r.querySelector('[data-poikkeama-checkbox]');
                                if (cb) cb.checked = selectAllVisible.checked;
                            });
                            apply();
                        });
                    }

                    document.querySelectorAll('[data-select-group]').forEach(groupCb => {
                        groupCb.addEventListener('change', () => {
                            const groupId = groupCb.dataset.selectGroup;
                            document.querySelectorAll(
                                'tr[data-solki-id="' + groupId + '"] [data-poikkeama-checkbox]'
                            ).forEach(cb => { cb.checked = groupCb.checked; });
                            apply();
                        });
                    });

                    apply();
                    """.trimIndent(),
                )
            }
        }
}
