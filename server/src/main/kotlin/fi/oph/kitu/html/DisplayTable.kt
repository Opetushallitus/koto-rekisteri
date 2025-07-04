package fi.oph.kitu.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.reverse
import fi.oph.kitu.toSymbol
import kotlinx.html.FlowContent
import kotlinx.html.TABLE
import kotlinx.html.TBODY
import kotlinx.html.a
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.apereo.cas.client.util.CommonUtils.urlEncode
import kotlin.collections.plus

data class DisplayTableColumn<T>(
    val label: String,
    val sortKey: String? = null,
    val width: String? = null,
    val testId: String? = null,
    val renderValue: FlowContent.(T) -> Unit,
)

interface DisplayTableEnum {
    val name: String
    val entityName: String?
    val uiHeaderValue: String
    val urlParam: String

    fun <T> withValue(renderValue: FlowContent.(T) -> Unit) =
        DisplayTableColumn(
            label = uiHeaderValue,
            sortKey = urlParam,
            renderValue = renderValue,
            testId = entityName,
        )
}

fun <T> TABLE.displayTableHeader(
    columns: List<DisplayTableColumn<T>>,
    sortedBy: DisplayTableEnum? = null,
    sortDirection: SortDirection? = null,
    urlParams: Map<String, String?> = emptyMap(),
    preserveSortDirection: Boolean,
) {
    val sortedByKey = sortedBy?.urlParam
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
                                                } else if (preserveSortDirection) {
                                                    SortDirection.ASC.name
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
}

fun <T> TABLE.displayTableBody(
    rows: List<T>,
    columns: List<DisplayTableColumn<T>>,
    rowTestId: ((T) -> String)? = null,
    rowClasses: String? = null,
    afterRow: TBODY.(T) -> Unit = {},
) {
    tbody {
        rows.forEach { row ->
            tr(classes = rowClasses) {
                testId(rowTestId?.let { it(row) })
                columns.forEach { column ->
                    td {
                        testId(column.testId)
                        column.renderValue(this, row)
                    }
                }
            }
            afterRow(this, row)
        }
    }
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
    table(classes = "striped") {
        testId(testId)
        debugTrace()
        displayTableHeader(
            columns = columns,
            sortedBy = sortedBy,
            sortDirection = sortDirection,
            urlParams = urlParams,
            preserveSortDirection = true,
        )

        displayTableBody(rows, columns, rowTestId)
    }
}

fun <K, V> httpParams(params: Map<K, V?>): String =
    "?${params
        .filter { (_, value) -> value != null }
        .map { (key, value) -> "$key=${urlEncode(value?.toString().orEmpty())}" }
        .joinToString("&")}"
