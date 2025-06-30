package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.httpParams
import fi.oph.kitu.html.input
import fi.oph.kitu.yki.html.Paging
import fi.oph.kitu.yki.html.errorsArticle
import fi.oph.kitu.yki.html.ykiTableHeader
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.button
import kotlinx.html.details
import kotlinx.html.fieldSet
import kotlinx.html.footer
import kotlinx.html.form
import kotlinx.html.header
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.summary
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr
import kotlinx.html.ul

object YkiSuorituksetPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        sortColumn: YkiSuoritusColumn,
        sortDirection: SortDirection,
        paging: Paging,
        versionHistory: Boolean,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs("/yki/suoritukset"),
        ) {
            fun FlowContent.navigationLink(
                search: String? = null,
                includeVersionHistory: Boolean? = null,
                page: Int? = null,
                sortColumnStr: String? = null,
                sortDirectionEnum: SortDirection? = null,
                ariaLabel: String? = null,
                innerText: String,
            ) {
                a(
                    href = "suoritukset?${
                        httpParams(
                            mapOf(
                                "search" to (search ?: paging.searchStrUrl),
                                "includeVersionHistory" to (includeVersionHistory ?: versionHistory),
                                "page" to (page ?: paging.currentPage),
                                "sortColumn" to (sortColumnStr ?: sortColumn.urlParam),
                                "sortDirection" to (sortDirectionEnum?.name ?: sortDirection.name),
                            ),
                        )}",
                ) {
                    if (ariaLabel != null) {
                        attributes["aria-label"] = ariaLabel
                    }

                    +innerText
                }
            }

            this.errorsArticle(errorsCount, "/yki/suoritukset/virheet")

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
                        value = paging.searchStr,
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
                                +"Suorituksia yhteensä: ${paging.totalEntries}"
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

                table {
                    ykiTableHeader(
                        "suoritukset",
                        paging,
                        versionHistory,
                        currentColumn = sortColumn,
                        sortDirection,
                    )

                    for (suoritus in suoritukset) {
                        tbody(classes = "suoritus") {
                            tr {
                                td { +suoritus.suorittajanOID.toString() }
                                td { +suoritus.sukunimi }
                                td { +suoritus.etunimet }
                                td { +suoritus.sukupuoli.name }
                                td { +suoritus.hetu }
                                td { +suoritus.kansalaisuus }
                                td { +"${suoritus.katuosoite}, ${suoritus.postinumero} ${suoritus.postitoimipaikka}" }
                                td { +suoritus.email.orEmpty() }
                                td { +suoritus.suoritusId }
                                td { +suoritus.tutkintopaiva.toString() }
                                td { +suoritus.tutkintokieli.name }
                                td { +suoritus.tutkintotaso.name }
                                td { +suoritus.jarjestajanTunnusOid.toString() }
                                td { +suoritus.jarjestajanNimi }
                                td { +suoritus.arviointipaiva.toString() }
                                td { +suoritus.tekstinYmmartaminen?.toString().orEmpty() }
                                td { +suoritus.kirjoittaminen?.toString().orEmpty() }
                                td { +suoritus.rakenteetJaSanasto?.toString().orEmpty() }
                                td { +suoritus.puheenYmmartaminen?.toString().orEmpty() }
                                td { +suoritus.puhuminen?.toString().orEmpty() }
                                td { +suoritus.yleisarvosana?.toString().orEmpty() }
                            }

                            if (suoritus.tarkistusarvioinninSaapumisPvm != null) {
                                tr {
                                    td {
                                        attributes["colspan"] = "13"
                                        details {
                                            summary { +"Näytä tarkistusarvioinnin tiedot" }
                                            table {
                                                tr {
                                                    th { +"Saapumispäivä" }
                                                    th { +"Asiatunnus" }
                                                    th { +"Osakokeet" }
                                                    th { +"Arvosana muuttui?" }
                                                    th { +"Perustelu" }
                                                    th { +"Käsittelypäivä" }
                                                }
                                                tr {
                                                    td { +suoritus.tarkistusarvioinninSaapumisPvm.toString() }
                                                    td { +suoritus.tarkistusarvioinninAsiatunnus.orEmpty() }
                                                    td { +suoritus.tarkistusarvioidutOsakokeet?.toString().orEmpty() }
                                                    td { +suoritus.arvosanaMuuttui?.toString().orEmpty() }
                                                    td { +suoritus.perustelu.orEmpty() }
                                                    td {
                                                        +suoritus.tarkistusarvioinninKasittelyPvm?.toString().orEmpty()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            footer {
                nav {
                    attributes["aria-label"] = "Suoritusten sivutus"

                    ul(classes = "paging") {
                        li {
                            // Navigate to previous page
                            navigationLink(
                                page = paging.previousPage,
                                ariaLabel = "Edellinen sivu",
                                innerText = "◀",
                            )
                        }
                        li {
                            // Show the number of the current page
                            +"${paging.currentPage}"
                        }
                        li {
                            // Navigate to next page
                            navigationLink(
                                page = paging.nextPage,
                                ariaLabel = "Seuraava sivu",
                                innerText = "▶",
                            )
                        }
                    }
                }
            }
        }
}
