package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTableBody
import fi.oph.kitu.html.displayTableHeader
import fi.oph.kitu.yki.html.errorsArticle
import kotlinx.html.article
import kotlinx.html.details
import kotlinx.html.h1
import kotlinx.html.h2
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
            wideContent = true,
        ) {
            h1 { +"Kotoutumiskoulutuksen kielitaidon päättötesti" }
            h2 { +"Suoritukset" }
            errorsArticle(errorsCount, linkTo(KielitestiViewController::virheetView).toString())

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
