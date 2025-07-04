package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.yki.html.errorsArticle
import kotlin.String
import kotlin.enums.enumEntries

object YkiArvioijaPage {
    fun render(
        arvioijat: List<YkiArvioijaEntity>,
        sortColumn: YkiArvioijaColumn,
        sortDirection: SortDirection,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs("/yki/arvioijat"),
            wideContent = true,
        ) {
            this.errorsArticle(errorsCount, "/yki/arvioijat/virheet")

            val columns =
                enumEntries<YkiArvioijaColumn>().map {
                    DisplayTableColumn(
                        label = it.uiHeaderValue,
                        sortKey = it.urlParam,
                        renderValue = it.renderValue,
                    )
                }

            displayTable(
                rows = arvioijat,
                columns = columns,
                sortedBy = sortColumn,
                sortDirection = sortDirection,
            )
        }
}
