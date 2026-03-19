package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.errorsArticle
import fi.oph.kitu.html.table.displayTableBody
import fi.oph.kitu.html.table.displayTableHeader
import fi.oph.kitu.html.testId
import fi.oph.kitu.organisaatiot.Organisaatiot
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.button
import kotlinx.html.details
import kotlinx.html.fieldSet
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.header
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.summary
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import org.springframework.hateoas.server.mvc.linkTo
import kotlin.enums.enumEntries

object KielitestiSuorituksetPage {
    fun render(
        sortColumn: KielitestiSuoritusColumn,
        sortDirection: SortDirection,
        suoritukset: List<KielitestiSuoritus>,
        errorsCount: Long,
        organisaationimet: Organisaatiot,
        search: String,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Kotoutumiskoulutuksen kielitaidon päättötesti" }
            h2 { +"Suoritukset" }
            errorsArticle(errorsCount, linkTo(KielitestiViewController::virheetView).toString())

            form(action = "", method = FormMethod.get, classes = "grid center-vertically") {
                fieldSet {
                    attributes["role"] = "search"
                    input {
                        testId("search")
                        id = "search"
                        type = InputType.search
                        name = "search"
                        value = search
                        placeholder = "Oppijanumero, nimi tai muu hakusana"
                    }
                    button {
                        testId("search-button")
                        type = ButtonType.submit
                        +"Suodata"
                    }
                }
            }

            article(classes = "overflow-auto") {
                header {
                    nav {
                        ul {
                            li {
                                +"Suorituksia yhteensä: ${suoritukset.size}"
                            }
                            li {
                                a(href = linkTo<KielitestiApiController> { getSuorituksetAsCsv() }.toString()) {
                                    attributes["download"] = ""
                                    +"Lataa tiedot CSV:nä"
                                }
                            }
                        }
                    }
                }

                table {
                    val columns =
                        enumEntries<KielitestiSuoritusColumn>().map {
                            it.withValue(it.getValue(organisaationimet))
                        }
                    displayTableHeader(
                        columns = columns,
                        sortedBy = sortColumn,
                        sortDirection = sortDirection,
                        preserveSortDirection = false,
                        selectableRows = false,
                        tableId = "kielitesti-suoritukset-table",
                        urlParams = mapOf("search" to search),
                    )
                    displayTableBody(
                        rows = suoritukset,
                        columns = columns,
                        rowClasses = "suoritus",
                        rowTestId = { "suoritus-summary-row" },
                    ) { suoritus ->
                        tr {
                            attributes["data-testid"] = "suoritus-details-row"

                            td {
                                attributes["colspan"] = "13"
                                details {
                                    summary { +"Näytä lisätiedot/tulokset" }
                                    table {
                                        thead {
                                            tr {
                                                th { +"Oppijanumero" }
                                                th { +"Luetun ymmärtäminen" }
                                                th { +"Kuullun ymmärtäminen" }
                                                th { +"Puhe" }
                                                th { +"Kirjoittaminen" }
                                            }
                                        }
                                        tbody {
                                            tr {
                                                td { +suoritus.oppijanumero.toString() }
                                                td { +suoritus.luetunYmmartaminen.toString() }
                                                td { +suoritus.kuullunYmmartaminen.toString() }
                                                td { +suoritus.puhe.toString() }
                                                td { +suoritus.kirjoittaminen.toString() }
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
