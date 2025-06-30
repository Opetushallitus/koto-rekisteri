package fi.oph.kitu.yki.html

import fi.oph.kitu.KituColumn
import fi.oph.kitu.SortDirection
import fi.oph.kitu.reverse
import fi.oph.kitu.toSymbol
import fi.oph.kitu.yki.suoritukset.YkiSuorituksetPage.toUrlParams
import kotlinx.html.TABLE
import kotlinx.html.a
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

inline fun <reified Header> TABLE.ykiTableHeader(
    page: String,
    paging: Paging? = null,
    versionHistory: Boolean? = null,
    currentColumn: Header,
    sortDirection: SortDirection,
) where Header : Enum<Header>, Header : KituColumn {
    thead {
        tr {
            for (column in enumValues<Header>()) {
                th {
                    a(
                        href = "$page?${mapOf(
                            "search" to paging?.searchStrUrl,
                            "includeVersionHistory" to versionHistory,
                            "page" to paging?.currentPage,
                            "sortColumn" to column.urlParam,
                            "sortDirection" to if (currentColumn == column) sortDirection.reverse() else sortDirection,
                        ).toUrlParams()}",
                    ) {
                        +"${column.uiHeaderValue} ${if (currentColumn == column) sortDirection.toSymbol() else ""}"
                    }
                }
            }
        }
    }
}
