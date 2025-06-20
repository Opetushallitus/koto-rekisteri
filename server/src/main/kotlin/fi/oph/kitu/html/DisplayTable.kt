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
    val testId: String? = null,
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
            testId = dbColumn,
        )
}

fun <T> FlowContent.displayTable(
    rows: List<T>,
    columns: List<DisplayTableColumn<T>>,
    sortedBy: DisplayTableEnum? = null,
    sortDirection: SortDirection? = null,
    testId: String? = null,
    rowTestId: ((T) -> String)? = null,
    urlParams: Map<String, String?> = emptyMap(),
) {
    val sortedByKey = sortedBy?.urlParam

    table(classes = "striped") {
        testId(testId)
        debugTrace()
        thead {
            tr {
                columns.forEach {
                    th {
                        testId(it.testId)
                        if (it.width != null) {
                            style = "width: ${it.width};"
                        }
                        if (it.sortKey != null && sortedBy != null && sortDirection != null) {
                            val isSortedColumn = it.sortKey == sortedByKey
                            a(
                                href =
                                    httpParams(
                                        urlParams +
                                            mapOf(
                                                "sortColumn" to it.sortKey,
                                                "sortDirection" to
                                                    if (isSortedColumn) {
                                                        sortDirection.reverse().name
                                                    } else {
                                                        SortDirection.ASC.name
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
                    testId(rowTestId?.let { it(row) })
                    columns.forEach { column ->
                        td {
                            testId(column.testId)
                            column.renderValue(this, row)
                        }
                    }
                }
            }
        }
    }
}

fun httpParams(params: Map<String, String?>): String =
    "?${params
        .filter { (_, value) -> value != null }
        .map { (key, value) -> "$key=${urlEncode(value)}" }
        .joinToString("&")}"
