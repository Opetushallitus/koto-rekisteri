package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableColumn
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
            breadcrumbs = Navigation.getBreadcrumbs("/yki/suoritukset/virheet"),
            wideContent = true,
        ) {
            val columns =
                enumValues<YkiSuoritusErrorColumn>().map { it ->
                    DisplayTableColumn(
                        label = it.uiHeaderValue,
                        sortKey = it.urlParam,
                        renderValue = it.renderValue,
                    )
                }

            displayTable(
                rows = virheet,
                columns = columns,
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                rowClasses = "virheet",
                rowTestId = { "error-row" },
            )
        }
}
