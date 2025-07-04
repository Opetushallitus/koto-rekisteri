package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page
import fi.oph.kitu.yki.html.errorsArticle
import kotlin.String

object YkiArvioijaPage {
    fun render(
        arvioijat: List<YkiArvioijaEntity>,
        sortColumn: YkiArvioijaColumn,
        sortDirection: SortDirection,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs("/yki/arvioijat"),
        ) {
            this.errorsArticle(errorsCount, "/yki/arvioijat/virheet")
        }
}
