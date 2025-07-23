package fi.oph.kitu.yki.suoritukset.error

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Navigation.navItem
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTable
import fi.oph.kitu.yki.YkiViewController
import org.springframework.hateoas.server.mvc.linkTo

object YkiSuoritusErrorPage {
    fun render(
        sortColumn: YkiSuoritusErrorColumn,
        sortDirection: SortDirection,
        virheet: List<YkiSuoritusErrorEntity>,
    ): String =
        Page.renderHtml(
            breadcrumbs =
                Navigation.getBreadcrumbs(
                    linkTo(YkiViewController::suorituksetGetView).toString(),
                    navItem(
                        "Virheet",
                        linkTo(YkiViewController::suorituksetVirheetView),
                    ),
                ),
            wideContent = true,
        ) {
            displayTable(
                rows = virheet,
                columns = enumValues<YkiSuoritusErrorColumn>().map { it.withValue(it.renderValue) },
                sortedBy = sortColumn,
                sortDirection = sortDirection,
                rowClasses = "virheet",
                rowTestId = { "error-row" },
            )
        }
}
