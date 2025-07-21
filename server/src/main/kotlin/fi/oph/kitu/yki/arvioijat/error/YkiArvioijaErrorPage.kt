package fi.oph.kitu.yki.arvioijat.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Navigation.navItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.yki.YkiViewController
import org.springframework.hateoas.server.mvc.linkTo

object YkiArvioijaErrorPage {
    fun render(
        sortColumn: YkiArvioijaErrorColumn,
        sortDirection: SortDirection,
        virheet: List<YkiArvioijaErrorEntity>,
    ): String =
        Page.renderHtml(
            breadcrumbs =
                Navigation.getBreadcrumbs(
                    linkTo(YkiViewController::arvioijatView).toString(),
                    navItem("Virheet", YkiViewController::arvioijatVirheetView),
                ),
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
