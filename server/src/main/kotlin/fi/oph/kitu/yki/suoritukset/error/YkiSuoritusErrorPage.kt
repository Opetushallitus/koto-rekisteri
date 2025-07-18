package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable

object YkiSuoritusErrorPage {
    fun render(
        sortColumn: YkiSuoritusErrorColumn,
        sortDirection: SortDirection,
        virheet: List<YkiSuoritusErrorEntity>,
    ): String =
        Page.renderHtml(
            breadcrumbs =
                Navigation.getBreadcrumbs(
                    "/yki/suoritukset",
                    Navigation.MenuItem(
                        "Virheet",
                        "/yki/suoritukset/virheet",
                    ),
                ),
            wideContent = true,
        ) {
            displayTable(
                rows = virheet,
                columns = enumValues<YkiSuoritusErrorColumn>().map { it.withValue(it.renderValue) },
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                rowClasses = "virheet",
                rowTestId = { "error-row" },
            )
        }
}
