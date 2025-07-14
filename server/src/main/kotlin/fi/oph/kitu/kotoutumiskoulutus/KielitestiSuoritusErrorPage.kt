package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.classes
import fi.oph.kitu.html.displayTableHeader
import kotlinx.html.article
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr
import kotlin.enums.enumEntries

object KielitestiSuoritusErrorPage {
    fun render(
        sortColumn: KielitestiSuoritusErrorColumn,
        sortDirection: SortDirection,
        errors: Iterable<KielitestiSuoritusError>,
    ): String =
        Page.renderHtml(
            breadcrumbs =
                Navigation.getBreadcrumbs(
                    "/koto-kielitesti/suoritukset",
                    Navigation.MenuItem(
                        "Virheet",
                        "/koto-kielitesti/suoritukset/virheet",
                    ),
                ),
            wideContent = true,
        ) {
            article(classes = "overflow-auto") {
                table(classes = "compact striped") {
                    val columns = enumEntries<KielitestiSuoritusErrorColumn>().map { it.withValue(it.renderValue) }
                    displayTableHeader(
                        columns = columns,
                        sortedBy = sortColumn,
                        sortDirection = sortDirection,
                        preserveSortDirection = false,
                    )

                    tbody(classes = "virheet") {
                        for (error in errors) {
                            tr {
                                attributes["data-testid"] = "virhe-summary-row"

                                td(classes = "truncated") {
                                    attributes["headers"] = "hetu"
                                    if (!error.hetu.isNullOrEmpty()) {
                                        span {
                                            attributes["title"] = error.hetu
                                            +error.hetu
                                        }
                                    }
                                }

                                td {
                                    attributes["headers"] = "nimi"
                                    +error.nimi
                                }

                                td {
                                    attributes["headers"] = "schoolOid"
                                    +error.schoolOid?.toString().orEmpty()
                                }

                                td {
                                    attributes["headers"] = "teacherEmail"
                                    +error.teacherEmail.orEmpty()
                                }

                                td {
                                    attributes["headers"] = "virheenLuontiaika"
                                    +error.virheenLuontiaika.toString()
                                }

                                td {
                                    attributes["headers"] = "viesti"
                                    +error.viesti
                                }

                                td {
                                    attributes["headers"] = "virheellinenKentta"
                                    +error.virheellinenKentta.orEmpty()
                                }

                                td {
                                    attributes["headers"] = "virheellinenArvo"
                                    +error.virheellinenArvo.orEmpty()
                                }
                            }
                        }
                    }
                }
            }
        }
}
