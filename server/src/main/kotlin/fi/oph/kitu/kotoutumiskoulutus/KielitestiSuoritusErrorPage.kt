package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTableBody
import fi.oph.kitu.html.displayTableHeader
import kotlinx.html.article
import kotlinx.html.table
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
                    KielitestiViewController::suorituksetView,
                    Navigation.MenuItem.of("Virheet", KielitestiViewController::virheetView),
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

                    displayTableBody(
                        rows = errors.toList(),
                        columns = columns,
                        tbodyClasses = "virheet",
                        rowTestId = { "virhe-summary-row" },
                    )
                }
            }
        }
}
