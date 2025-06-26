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
import kotlinx.html.TR
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
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import java.net.URLEncoder
import kotlin.math.ceil

data class Paging(
    val totalEntries: Long,
    val limit: Int,
    val currentPage: Int,
    val searchStr: String,
) {
    val totalPages = ceil(totalEntries.toDouble() / limit).toInt()
    val nextPage = if (currentPage >= totalPages) null else currentPage + 1
    val previousPage = if (currentPage <= 1) null else currentPage - 1
    val searchStrUrl: String = URLEncoder.encode(searchStr, Charsets.UTF_8)
}

object YkiSuorituksetPage {
    fun render(
        suoritukset: List<YkiSuoritusEntity>,
        header: List<HeaderCell<YkiSuoritusColumn>>,
        sortColumn: String,
        sortDirection: SortDirection,
        paging: Paging,
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
                        thead {
                            tr {
                                for (cell in header) {
                                    th {
                                        a(
                                            href = "suoritukset?${
                                                mapOf(
                                                    "search" to paging.searchStrUrl,
                                                    "includeVersionHistory" to versionHistory,
                                                    "sortColumn" to cell.column.urlParam,
                                                    "sortDirection" to cell.sortDirection,
                                                ).toUrlParams()}",
                                        ) {
                                            +"${cell.column.uiHeaderValue} ${cell.symbol}"
                                        }
                                    }
                                }
                            }
                        }
                        for (suoritus in suoritukset) {
                            tbody(classes = "suoritus") {
                                tr {
                                    cell(suoritus.suorittajanOID)
                                    cell(suoritus.sukunimi)
                                    cell(suoritus.etunimet)
                                    cell(suoritus.sukupuoli.name)
                                    cell(suoritus.hetu)
                                    cell(suoritus.kansalaisuus)
                                    cell("${suoritus.katuosoite}, ${suoritus.postinumero} ${suoritus.postitoimipaikka}")
                                    cell(suoritus.email)
                                    cell(suoritus.suoritusId)
                                    cell(suoritus.tutkintopaiva)
                                    cell(suoritus.tutkintokieli.name)
                                    cell(suoritus.tutkintotaso.name)
                                    cell(suoritus.jarjestajanTunnusOid)
                                    cell(suoritus.jarjestajanNimi)
                                    cell(suoritus.arviointipaiva)
                                    cell(suoritus.tekstinYmmartaminen)
                                    cell(suoritus.kirjoittaminen)
                                    cell(suoritus.rakenteetJaSanasto)
                                    cell(suoritus.puheenYmmartaminen)
                                    cell(suoritus.puhuminen)
                                    cell(suoritus.yleisarvosana)
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
                                                        cell(suoritus.tarkistusarvioinninSaapumisPvm)
                                                        cell(suoritus.tarkistusarvioinninAsiatunnus)
                                                        cell(suoritus.tarkistusarvioidutOsakokeet)
                                                        cell(suoritus.arvosanaMuuttui)
                                                        cell(suoritus.perustelu)
                                                        cell(suoritus.tarkistusarvioinninKasittelyPvm)
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
                                a(
                                    href = "suoritukset?${mapOf(
                                        "search" to paging.searchStrUrl,
                                        "includeVersionHistory" to versionHistory,
                                        "page" to paging.previousPage,
                                        "sortColumn" to sortColumn,
                                        "sortDirection" to sortDirection,
                                    ).toUrlParams()}",
                                ) {
                                    attributes["aria-label"] = "Edellinen sivu"
                                    +"◀"
                                }
                            }
                        }
                        li {
                            // Show the number of the current page
                            +"${paging.currentPage}"
                        }
                        li {
                            // Navigate to next page
                            a(
                                href = "suoritukset?${mapOf(
                                    "search" to paging.searchStrUrl,
                                    "includeVersionHistory" to versionHistory,
                                    "page" to paging.nextPage,
                                    "sortColumn" to sortColumn,
                                    "sortDirection" to sortDirection,
                                ).toUrlParams()}",
                            ) {
                                attributes["aria-label"] = "Seuraava sivu"
                                +"▶"
                            }
                        }
                    }
                }
            }
        }


    fun <K, V> Map<K, V>.toUrlParams(): String =
        this
            .map { entry -> entry }
            .joinToString(separator = "&") {
                listOf(it.key, it.value).joinToString(separator = "=") { it ->
                    URLEncoder.encode(
                        it.toString(),
                        "UTF-8",
                    )
                }
            }

    fun <T> TR.cell(value: T? = null) {
        td {
            +(value?.toString() ?: "")
        }
    }
}
