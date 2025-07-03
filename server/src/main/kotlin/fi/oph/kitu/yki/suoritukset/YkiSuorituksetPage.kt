package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.displayTableHeader
import fi.oph.kitu.html.error
import fi.oph.kitu.html.input
import fi.oph.kitu.html.pagination
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.br
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
import kotlin.enums.enumEntries

object YkiSuorituksetPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        sortColumn: YkiSuoritusColumn,
        sortDirection: SortDirection,
        pagination: Pagination,
        search: String,
        versionHistory: Boolean,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            breadcrumbs = Navigation.getBreadcrumbs("/yki/suoritukset"),
            wideContent = true,
        ) {
            if (errorsCount > 0) {
                error("Järjestelmässä on $errorsCount virhettä.") {
                    br()
                    a("/yki/suoritukset/virheet") {
                        +"Katso virheet"
                    }
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
                        value = search,
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
                                +"Suorituksia yhteensä: ${pagination.numberOfPages}"
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
                    displayTableHeader(
                        columns =
                            enumEntries<YkiSuoritusColumn>().map {
                                DisplayTableColumn<YkiSuoritusEntity>(
                                    label = it.uiHeaderValue,
                                    sortKey = it.urlParam,
                                    renderValue = {},
                                )
                            },
                        sortedBy = sortColumn,
                        sortDirection = sortDirection,
                        urlParams =
                            mapOf(
                                "search" to search,
                                "includeVersionHistory" to "$versionHistory",
                                "page" to "${pagination.currentPageNumber}",
                            ),
                        preserveSortDirection = false,
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
                                td { +suoritus.suoritusId.toString() }
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
                                                    td {
                                                        +suoritus.tarkistusarvioinninSaapumisPvm?.toString().orEmpty()
                                                    }
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
                pagination(pagination)
            }
        }
}
