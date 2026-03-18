package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.errorsArticle
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.input
import fi.oph.kitu.html.koskiErrorsArticle
import fi.oph.kitu.html.pagination
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.dateFilter
import fi.oph.kitu.html.table.displayTableBody
import fi.oph.kitu.html.table.displayTableHeader
import fi.oph.kitu.html.table.enumFilter
import fi.oph.kitu.html.table.httpParams
import fi.oph.kitu.html.table.tableFilterDialog
import fi.oph.kitu.html.table.toggleFilter
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.YkiApiController
import fi.oph.kitu.yki.YkiSuorituksetParams
import fi.oph.kitu.yki.YkiViewController
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.button
import kotlinx.html.fieldSet
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.header
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.table
import kotlinx.html.ul
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.security.web.csrf.CsrfToken

object YkiSuorituksetPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        totalSuoritukset: Long,
        params: YkiSuorituksetParams,
        pagination: Pagination,
        errorsCount: Long,
        koskiErrorsCount: Long,
        csrfToken: CsrfToken?,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Yleinen kielitutkinto" }
            h2 { +"Suoritukset" }
            errorsArticle(
                errorsCount,
                linkTo(YkiViewController::suorituksetVirheetView).toString(),
            )
            koskiErrorsArticle(
                koskiErrorsCount,
                linkTo(YkiViewController::koskiVirheetView).toString(),
            )

            formPost(
                action = "",
                csrfToken = csrfToken,
                formClasses = "grid center-vertically",
            ) {
                fieldSet {
                    attributes["role"] = "search"
                    input(
                        id = "search",
                        type = InputType.text,
                        name = "search",
                        value = params.search,
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
                            checked = params.versionHistory,
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
                            li { +"Suorituksia yhteensä: $totalSuoritukset" }
                            li { ykiSuoritusFilters(params) }
                            li { csvDownloadButton(params) }
                        }
                    }
                }

                table {
                    val columns =
                        DisplayTableColumn.of<YkiSuoritusColumn, YkiSuoritusEntity>(setOf(ColumnTag.LIST_VIEW))

                    displayTableHeader(
                        columns = columns,
                        sortedBy = params.sortColumn,
                        sortDirection = params.sortDirection,
                        urlParams = params.toMap().plus("page" to pagination.currentPageNumber.toString()),
                        preserveSortDirection = false,
                        selectableRows = false,
                        tableId = "suoritukset-table",
                    )

                    displayTableBody(
                        rows = suoritukset,
                        columns = columns,
                        rowClasses = "suoritus",
                    ) { suoritus ->
//                        if (suoritus.tarkistusarvioinninSaapumisPvm != null) {
//                            tr {
//                                td {
//                                    attributes["colspan"] = "13"
//                                    details {
//                                        summary { +"Näytä tarkistusarvioinnin tiedot" }
//                                        table {
//                                            tr {
//                                                th { +"Saapumispäivä" }
//                                                th { +"Asiatunnus" }
//                                                th { +"Osakokeet" }
//                                                th { +"Arvosana muuttui?" }
//                                                th { +"Perustelu" }
//                                                th { +"Käsittelypäivä" }
//                                                th { +"Tutkintotoimikunnan hyväksyntä" }
//                                            }
//                                            tr {
//                                                td {
//                                                    +suoritus.tarkistusarvioinninSaapumisPvm.toString()
//                                                }
//                                                td { +suoritus.tarkistusarvioinninAsiatunnus.orEmpty() }
//                                                td {
//                                                    +suoritus.tarkistusarvioidutOsakokeet
//                                                        ?.joinToString(", ") { it.viewText }
//                                                        .orEmpty()
//                                                }
//                                                td {
//                                                    +suoritus.arvosanaMuuttui
//                                                        ?.joinToString(", ") { it.viewText }
//                                                        .orEmpty()
//                                                }
//                                                td { +suoritus.perustelu.orEmpty() }
//                                                td {
//                                                    +suoritus.tarkistusarvioinninKasittelyPvm?.toString().orEmpty()
//                                                }
//                                                td {
//                                                    +suoritus.tarkistusarviointiHyvaksyttyViewText().orEmpty()
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
                    }
                }
            }

            footer {
                pagination(pagination)
            }
        }
}

fun FlowContent.csvDownloadButton(params: YkiSuorituksetParams) {
    a(
        href =
            linkTo<YkiApiController> {
                getSuorituksetAsCsv()
            }.toString() + "?${httpParams(params.toMap())}",
    ) {
        attributes["download"] = ""
        +"Lataa tiedot CSV:nä"
    }
}

fun FlowContent.ykiSuoritusFilters(params: YkiSuorituksetParams) {
    tableFilterDialog("suoritukset") {
        fieldSet(classes = "grid") {
            dateFilter("tutkintoalku", "Tutkintopäivä alkaen", params.tutkintoalku)
            dateFilter("tutkintoloppu", "Tutkintopäivä päättyen", params.tutkintoloppu)
        }
        fieldSet {
            enumFilter<Tutkintokieli>("tutkintokieli", "Tutkintokieli", params.tutkintokieli)
        }
        fieldSet {
            enumFilter<Tutkintotaso>("tutkintotaso", "Tutkintotaso", params.tutkintotaso)
        }
        fieldSet {
            toggleFilter("piilotaHenkilotiedot", "Piilota henkilötiedot CSV:llä", params.piilotaHenkilotiedot)
        }
    }
}
