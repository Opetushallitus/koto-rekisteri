package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.HeaderCell
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Navigation
import fi.oph.kitu.html.Page

object YkiSuorituksetPage {
    fun <T> render(
        suoritukset: List<YkiSuoritusEntity>,
        header: List<HeaderCell<YkiSuoritusColumn>>,
        sortColumn: String,
        sortDirection: SortDirection,
        paging: T,
        versionHistory: Boolean,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            Navigation.getBreadcrumbs("/yki/suoritukset"),
        ) {
        }
}
