package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.HeaderCell
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.yki.html.errorsArticle
import fi.oph.kitu.yki.html.ykiTableHeader
import kotlinx.html.article
import kotlinx.html.table

object YkiArvioijaPage {
    fun render(
        arvioijat: List<YkiArvioijaEntity>,
        header: List<HeaderCell<YkiArvioijaColumn>>,
        sortColumn: String,
        sortDirection: SortDirection,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs("/yki/arvioijat"),
        ) {
            this.errorsArticle(errorsCount, "/yki/arvioijat/virheet")

            article(classes = "overflow-auto") {
                table {
                    ykiTableHeader(header, sortDirection = sortDirection)
                }
            }
        }
}
