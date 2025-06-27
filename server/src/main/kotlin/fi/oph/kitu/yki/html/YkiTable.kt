package fi.oph.kitu.yki.html

import fi.oph.kitu.HeaderCell
import fi.oph.kitu.KituColumn
import fi.oph.kitu.SortDirection
import fi.oph.kitu.yki.suoritukset.YkiSuorituksetPage.cell
import fi.oph.kitu.yki.suoritukset.YkiSuorituksetPage.toUrlParams
import kotlinx.html.TABLE
import kotlinx.html.a
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

fun <Header> TABLE.ykiTableHeader(
    header: List<HeaderCell<Header>>,
    paging: Paging? = null,
    versionHistory: Boolean? = null,
    sortDirection: SortDirection,
) where Header : Enum<Header>, Header : KituColumn {
    thead {
        tr {
            for (cell in header) {
                th {
                    a(
                        href = "suoritukset?${mapOf(
                            "search" to paging?.searchStrUrl,
                            "includeVersionHistory" to versionHistory,
                            "page" to paging?.currentPage,
                            "sortColumn" to cell.column.urlParam,
                            "sortDirection" to sortDirection,
                        ).toUrlParams()}",
                    ) {
                        +"${cell.column.uiHeaderValue} ${cell.symbol}"
                    }
                }
            }
        }
    }
}
