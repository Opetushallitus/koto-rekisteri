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
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.i18n.unaryPlus
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
            h1 { +UiText.Nav.yki }
            h2 { +UiText.Yki.suoritustenPoikkeamat }

            viewMessage(message)

            if (poikkeamat.isEmpty()) {
                p { +UiText.Yki.eiPoikkeamia }
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
                            +UiText.Yki.tallennaKorjaukset
                        }
                    }

                    article(classes = "overflow-auto") {
                        table(classes = "striped") {
                            thead {
                                tr {
                                    th { +UiText.Yki.Sarake.solkiId }
                                    th {
                                        +UiText.Yki.Sarake.tutkintopaiva
                                        checkboxDropdown(
                                            title = UiText.Yki.suodata.toString(),
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
                                        +UiText.Yki.Sarake.kieli
                                        checkboxDropdown(
                                            title = UiText.Yki.suodata.toString(),
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
                                        +UiText.Yki.Sarake.taso
                                        checkboxDropdown(
                                            title = UiText.Yki.suodata.toString(),
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
                                        +UiText.Yki.Sarake.kentta
                                        checkboxDropdown(
                                            title = UiText.Yki.suodata.toString(),
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
                                    th { +UiText.Yki.Sarake.arvoKitussa }
                                    th { +UiText.Yki.Sarake.arvoSolkissa }
                                    th { +UiText.Yki.Sarake.havaittu }
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
                    const rows = Array.from(document.querySelectorAll('tbody tr'));
                    const patchButton = document.querySelector('[data-patch-button]');
                    const selectAllVisible = document.querySelector('[data-select-all-visible]');

                    const groups = new Map();
                    function groupOf(id) {
                        let g = groups.get(id);
                        if (!g) { g = { groupCbs: [], memberCbs: [] }; groups.set(id, g); }
                        return g;
                    }
                    rows.forEach(r => {
                        const g = groupOf(r.dataset.solkiId);
                        r.querySelectorAll('[data-select-group]').forEach(cb => g.groupCbs.push(cb));
                        r.querySelectorAll('[data-poikkeama-checkbox]').forEach(cb => g.memberCbs.push(cb));
                    });

                    function updatePatchButton() {
                        if (patchButton) {
                            patchButton.disabled = !document.querySelector('[data-poikkeama-checkbox]:checked');
                        }
                    }

                    function updateGroupState(id) {
                        const g = groups.get(id);
                        if (!g) return;
                        const total = g.memberCbs.length;
                        const checked = g.memberCbs.filter(cb => cb.checked).length;
                        g.groupCbs.forEach(groupCb => {
                            groupCb.checked = checked > 0 && checked === total;
                            groupCb.indeterminate = checked > 0 && checked < total;
                        });
                    }

                    function applyFilter() {
                        const active = Array.from(dropdowns)
                            .map(d => ({
                                key: d.dataset.filterKey,
                                values: new Set(
                                    Array.from(d.querySelectorAll('input[type=checkbox]:checked')).map(c => c.value)
                                ),
                            }))
                            .filter(f => f.values.size > 0);

                        let prevSolkiId = null;
                        rows.forEach(r => {
                            const visible = active.every(f => f.values.has(r.getAttribute('data-' + f.key)));
                            r.hidden = !visible;
                            if (visible) {
                                const id = r.dataset.solkiId;
                                r.classList.toggle('repeat-group', id === prevSolkiId);
                                prevSolkiId = id;
                            }
                        });
                    }

                    dropdowns.forEach(d => d.addEventListener('change', applyFilter));

                    groups.forEach((g, id) => {
                        g.memberCbs.forEach(cb => cb.addEventListener('change', () => {
                            updateGroupState(id);
                            updatePatchButton();
                        }));
                        g.groupCbs.forEach(groupCb => groupCb.addEventListener('change', () => {
                            g.memberCbs.forEach(cb => { cb.checked = groupCb.checked; });
                            updateGroupState(id);
                            updatePatchButton();
                        }));
                    });

                    if (selectAllVisible) {
                        selectAllVisible.addEventListener('change', () => {
                            const affected = new Set();
                            rows.forEach(r => {
                                if (r.hidden) return;
                                const cb = r.querySelector('[data-poikkeama-checkbox]');
                                if (cb) {
                                    cb.checked = selectAllVisible.checked;
                                    affected.add(r.dataset.solkiId);
                                }
                            });
                            affected.forEach(updateGroupState);
                            updatePatchButton();
                        });
                    }

                    applyFilter();
                    groups.forEach((g, id) => updateGroupState(id));
                    updatePatchButton();
                    """.trimIndent(),
                )
            }
        }
}
