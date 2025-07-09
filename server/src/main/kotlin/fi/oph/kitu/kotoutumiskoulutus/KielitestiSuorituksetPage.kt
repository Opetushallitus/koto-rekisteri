package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTableBody
import fi.oph.kitu.html.displayTableHeader
import fi.oph.kitu.yki.html.errorsArticle
import kotlinx.html.article
import kotlinx.html.details
import kotlinx.html.summary
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr
import kotlin.enums.enumEntries

object KielitestiSuorituksetPage {
    fun render(
        sortColumn: KielitestiSuoritusColumn,
        sortDirection: SortDirection,
        suoritukset: List<KielitestiSuoritus>,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            breadcrumbs = Navigation.getBreadcrumbs("/koto-kielitesti/suoritukset"),
            wideContent = true,
        ) {
            errorsArticle(errorsCount, "/koto-kielitesti/suoritukset/virheet")

            article(classes = "overflow-auto") {
                table {
                    val columns = enumEntries<KielitestiSuoritusColumn>().map { it.withValue(it.renderValue) }
                    displayTableHeader(
                        columns = columns,
                        sortedBy = sortColumn,
                        sortDirection = sortDirection,
                        preserveSortDirection = false,
                    )
                    displayTableBody(
                        rows = suoritukset,
                        columns = columns,
                        rowClasses = "suoritus",
                        rowTestId = { "suoritus-summary-row" },
                    ) { suoritus ->
                        tr {
                            attributes["colspan"] = "13"

                            td {
                                attributes["rowspan"] = "13"
                                details {
                                    summary { +"Näytä lisätiedot/tulokset" }
                                    table {
                                        tr {
                                            th { +"Oppijanumero" }
                                            th { +"Luetun ymmärtäminen" }
                                            th { +"Kuullun ymmärtäminen" }
                                            th { +"Puhe" }
                                            th { +"Kirjoittaminen" }
                                        }
                                        tr {
                                            td { +suoritus.oppijanumero.toString() }
                                            td { +suoritus.luetunYmmartaminenResult }
                                            td { +suoritus.kuullunYmmartaminenResult }
                                            td { +suoritus.puheResult }
                                            td { +suoritus.kirjoittaminenResult.orEmpty() }
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
