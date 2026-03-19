package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.table.displayTable
import kotlinx.html.h1
import kotlinx.html.h2

object YkiArvioijaErrorPage {
    fun render(
        sortColumn: YkiArvioijaErrorColumn,
        sortDirection: SortDirection,
        virheet: List<YkiArvioijaErrorEntity>,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Yleinen kielitutkinto" }
            h2 { +"Arvioijien tuonnin virheet" }
            displayTable(
                rows = virheet,
                columns = enumValues<YkiArvioijaErrorColumn>().map { it.withValue(it.getValue) },
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                rowClasses = "virheet",
                rowTestId = { "error-row" },
            )
        }
}
