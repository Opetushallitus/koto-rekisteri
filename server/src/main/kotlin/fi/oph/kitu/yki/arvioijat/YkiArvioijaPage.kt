package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.yki.YkiViewController
import fi.oph.kitu.yki.html.errorsArticle
import kotlinx.html.article
import kotlinx.html.h1
import kotlinx.html.h2
import org.springframework.hateoas.server.mvc.linkTo
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
            wideContent = true,
        ) {
            h1 { +"Yleiset kielitutkinnot" }
            h2 { +"Arvioijat" }
            this.errorsArticle(errorsCount, linkTo(YkiViewController::arvioijatVirheetView).toString())

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
