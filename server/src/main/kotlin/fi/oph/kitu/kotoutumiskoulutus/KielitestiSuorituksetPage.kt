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
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.springframework.hateoas.server.mvc.linkTo
import kotlin.enums.enumEntries

object KielitestiSuorituksetPage {
    fun render(
        sortColumn: KielitestiSuoritusColumn,
        sortDirection: SortDirection,
        suoritukset: List<KielitestiSuoritus>,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            breadcrumbs =
                Navigation.getBreadcrumbs(
                    linkTo(KielitestiViewController::suorituksetView).toString(),
                ),
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
}
