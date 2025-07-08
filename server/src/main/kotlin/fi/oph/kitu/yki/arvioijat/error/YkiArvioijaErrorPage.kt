package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.SortDirection
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
            displayTable(
                rows = virheet,
                columns = enumValues<YkiArvioijaErrorColumn>().map { it.withValue(it.renderValue) },
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                rowClasses = "virheet",
                rowTestId = { "error-row" },
            )
        }
}
