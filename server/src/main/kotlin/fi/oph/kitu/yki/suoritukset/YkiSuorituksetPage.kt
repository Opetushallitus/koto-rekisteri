package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.HeaderCell
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.error
import fi.oph.kitu.html.input
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.fieldSet
import kotlinx.html.form
import kotlinx.html.header
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.ul

object YkiSuorituksetPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        header: List<HeaderCell<YkiSuoritusColumn>>,
        sortColumn: String,
        sortDirection: SortDirection,
        paging: Map<String, String>,
        versionHistory: Boolean,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs("/yki/suoritukset"),
        ) {
            if (errorsCount > 0) {
                error("Järjestelmässä on $errorsCount virhettä.") {
                    br()
                    a("/yki/suoritukset/virheet") {
                        +"Katso virheet"
                    }
                }

                form(
                    action = "",
                    method = FormMethod.get,
                    classes = "grid center-vertically",
                ) {
                    fieldSet {
                        attributes["role"] = "search"
                        input(
                            id = "search",
                            type = InputType.text,
                            name = "search",
                            // TODO: Maybe we should create a class for paging, so that we can minimize typo errors
                            value =
                                paging["searchStr"]
                                    ?: throw RuntimeException("Missing 'searchStr' field from paging."),
                            placeholder = "Oppijanumero, henkilötunnus tai hakusana",
                        ) {
                            button(type = ButtonType.submit) {
                                +"Suodata"
                            }
                        }
                    }
                    fieldSet {
                        label {
                            input(
                                id = "versionHistory",
                                type = InputType.checkBox,
                                name = "versionHistory",
                                checked = versionHistory,
                            ) {
                                +"Näytä versiohistoria"
                            }
                        }
                    }
                }

                article(classes = "overflow-auto") {
                    header {
                        nav {
                            ul {
                                li {
                                    +"Suorituksia yhteensä: ${paging["totalEntries"]}" // TODO: Paging class
                                }
                                li {
                                    a(
                                        href = "/yki/api/suoritukset?includeVersionHistory=$versionHistory",
                                    ) {
                                        attributes["download"] = ""
                                        +"Lataa tiedot CSV:nä"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
