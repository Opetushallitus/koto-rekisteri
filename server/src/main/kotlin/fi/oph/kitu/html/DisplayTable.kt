package fi.oph.kitu.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.reverse
import fi.oph.kitu.toSymbol
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.apereo.cas.client.util.CommonUtils.urlEncode

data class DisplayTableColumn<T>(
    val label: String,
    val sortKey: String? = null,
    val width: String? = null,
    val renderValue: FlowContent.(T) -> Unit,
)

interface DisplayTableEnum {
    val name: String
    val dbColumn: String?
    val uiHeaderValue: String
    val urlParam: String

    fun <T> withValue(renderValue: FlowContent.(T) -> Unit) =
        DisplayTableColumn(
            label = uiHeaderValue,
            sortKey = urlParam,
            renderValue = renderValue,
        )
}

fun <T> FlowContent.displayTable(
    rows: List<T>,
    columns: List<DisplayTableColumn<T>>,
    sortedBy: DisplayTableEnum? = null,
    sortDirection: SortDirection? = null,
    compact: Boolean = false,
) {
    val sortedByKey = sortedBy?.urlParam

    table(classes = "${if (compact) "compact" else ""} striped") {
        debugTrace()
        thead {
            tr {
                columns.forEach {
                    th {
                        if (it.width != null) {
                            style = "width: ${it.width};"
                        }
                        if (it.sortKey != null && sortedBy != null && sortDirection != null) {
                            val isSortedColumn = it.sortKey == sortedByKey
                            a(
                                href =
                                    httpParams(
                                        mapOf(
                                            "sortColumn" to it.sortKey,
                                            "sortDirection" to
                                                if (isSortedColumn) {
                                                    sortDirection.reverse().name
                                                } else {
                                                    sortDirection.name
                                                },
                                        ),
                                    ),
                            ) {
                                +it.label
                                if (isSortedColumn) {
                                    +" ${sortDirection.toSymbol()}"
                                }
                            }
                        } else {
                            +it.label
                        }
                    }
                }
            }
        }
        tbody {
            rows.forEach { row ->
                tr {
                    columns.forEach { column ->
                        td { column.renderValue(this, row) }
                    }
                }
            }
        }
    }
}

fun httpParams(params: Map<String, String>): String =
    "?${params.map { (key, value) -> "$key=${urlEncode(value)}" }.joinToString("&")}"
