package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.yki.html.errorsArticle
import kotlinx.html.article
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

            article(classes = "overflow-auto") {
                displayTable(
                    rows = arvioijat,
                    columns = enumEntries<YkiArvioijaColumn>().map { it.withValue(it.renderValue) },
                    sortedBy = sortColumn,
                    sortDirection = sortDirection,
                )
            }
        }
}
