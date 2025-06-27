package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.HeaderCell
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.yki.errorsArticle

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
        }
}
