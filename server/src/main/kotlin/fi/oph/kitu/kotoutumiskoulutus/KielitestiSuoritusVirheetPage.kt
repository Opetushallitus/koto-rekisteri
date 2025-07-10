package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
import kotlinx.html.article

object KielitestiSuoritusVirheetPage {
    fun render(
        sortColumn: KielitestiSuoritusErrorColumn,
        sortDirection: SortDirection,
        virheet: Iterable<KielitestiSuoritusError>,
    ): String =
        Page.renderHtml(
            breadcrumbs =
                Navigation.getBreadcrumbs(
                    "/yki/arvioijat",
                    Navigation.MenuItem(
                        "Virheet",
                        "/yki/arvioijat/virheet",
                    ),
                ),
            wideContent = true,
        ) {
            article(classes = "overflow-auto") {
                displayTable(
                    rows = virheet.toList(),
                    columns = enumValues<KielitestiSuoritusErrorColumn>().map { it.withValue(it.renderValue) },
                    sortedBy = sortColumn,
                    sortDirection = sortDirection,
                    rowClasses = "virheet",
                    rowTestId = { "virhe-summary-row" },
                )
            }
        }
}
