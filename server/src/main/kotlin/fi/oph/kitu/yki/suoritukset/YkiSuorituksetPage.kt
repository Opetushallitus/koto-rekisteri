package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.displayTableBody
import fi.oph.kitu.html.displayTableHeader
import fi.oph.kitu.html.formPost
import fi.oph.kitu.html.input
import fi.oph.kitu.html.pagination
import fi.oph.kitu.yki.YkiApiController
import fi.oph.kitu.yki.YkiViewController
import fi.oph.kitu.yki.html.errorsArticle
import fi.oph.kitu.yki.html.koskiErrorsArticle
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.button
import kotlinx.html.details
import kotlinx.html.fieldSet
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.header
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.summary
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr
import kotlinx.html.ul
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.security.web.csrf.CsrfToken
import kotlin.enums.enumEntries

object YkiSuorituksetPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        totalSuoritukset: Long,
        sortColumn: YkiSuoritusColumn,
        sortDirection: SortDirection,
        pagination: Pagination,
        search: String,
        versionHistory: Boolean,
        errorsCount: Long,
        koskiErrorsCount: Long,
        csrfToken: CsrfToken,
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
                                +"Suorituksia yhteensä: $totalSuoritukset"
                            }
                            li {
                                a(href = linkTo<YkiApiController> { getSuorituksetAsCsv(versionHistory) }.toString()) {
                                    attributes["download"] = ""
                                    +"Lataa tiedot CSV:nä"
                                }
                            }
                        }
                    }
                }

                table {
                    val columns = enumEntries<YkiSuoritusColumn>().map { it.withValue(it.renderValue) }

                    displayTableHeader(
                        columns = columns,
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

                    displayTableBody(
                        rows = suoritukset,
                        columns = columns,
                        rowClasses = "suoritus",
                    ) { suoritus ->
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
                                                    +suoritus.tarkistusarvioinninSaapumisPvm.toString()
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

            footer {
                pagination(pagination)
            }
        }
}
