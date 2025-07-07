package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable

object YkiArvioijaErrorPage {
    fun render(
        sortColumn: YkiArvioijaErrorColumn,
        sortDirection: SortDirection,
        virheet: List<YkiArvioijaErrorEntity>,
    ): String =
        Page.renderHtml(
            breadcrumbs = Navigation.getBreadcrumbs("/yki/arvioijat/virheet"),
            wideContent = true,
        ) {
            val columns =
                enumValues<YkiArvioijaErrorColumn>().map { it ->
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
