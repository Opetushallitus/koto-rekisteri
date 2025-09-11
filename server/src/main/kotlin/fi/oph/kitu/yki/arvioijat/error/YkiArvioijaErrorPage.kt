package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
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
            h1 { +"Yleiset kielitutkinnot" }
            h2 { +"Arvioijien tuonnin virheet" }
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
