package fi.oph.kitu.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.reverse
import fi.oph.kitu.toSymbol
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.apereo.cas.client.util.CommonUtils.urlEncode

data class DisplayTableColumn<T>(
    val label: String,
    val key: String,
    val getValue: (T) -> String?,
)

interface DisplayTableEnum {
    val name: String
    val dbColumn: String
    val uiHeaderValue: String

    fun toLowercase(): String = name.lowercase()

    fun <T> withValue(getValue: (T) -> String?) =
        DisplayTableColumn<T>(
            label = uiHeaderValue,
            key = dbColumn,
            getValue = getValue,
        )
}

fun <T> FlowContent.displayTable(
    rows: List<T>,
    columns: List<DisplayTableColumn<T>>,
    sortedBy: DisplayTableEnum,
    sortDirection: SortDirection,
) {
    val sortedByKey = sortedBy.toLowercase()

    article(classes = "overflow-auto") {
        table(classes = "compact striped") {
            thead {
                tr {
                    columns.forEach {
                        val isSortedColumn = it.key == sortedByKey
                        th {
                            a(
                                href =
                                    httpParams(
                                        mapOf(
                                            "sortColumn" to it.key,
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
                                    +" "
                                    +sortDirection.toSymbol()
                                }
                            }
                        }
                    }
                }
            }
            tbody {
                rows.forEach { row ->
                    tr {
                        columns.forEach { column ->
                            td {
                                +(column.getValue(row) ?: "")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun httpParams(params: Map<String, String>): String =
    "?${params.map { (key, value) -> "$key=${urlEncode(value)}" }.joinToString("&")}"
