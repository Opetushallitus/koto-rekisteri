package fi.oph.kitu.html

import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPage.orDash
import kotlinx.html.FlowContent
import kotlinx.html.stream.createHTML
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.unsafe

fun FlowContent.infoTable(vararg rows: Pair<Any, FlowContent.() -> Unit>?) {
    table(classes = "info-table compact striped") {
        debugTrace()
        tbody {
            rows.filterNotNull().forEach { (name, render) ->
                tr {
                    th { +name.toString() }
                    td { render() }
                }
            }
        }
    }
}

fun FlowContent.comparisonTable(
    firstColumn: LocalizedString,
    secondColumn: LocalizedString,
    vararg rows: Triple<Any, FlowContent.() -> Unit, (FlowContent.() -> Unit)?>?,
) {
    table(classes = "info-table compact striped") {
        debugTrace()
        thead {
            tr {
                th {}
                th { +firstColumn.toString() }
                th { +secondColumn.toString() }
            }
        }
        tbody {
            rows.filterNotNull().forEach { (name, renderFirst, renderSecond) ->
                val firstTd = createHTML().td { renderFirst() }
                val secondTd = createHTML().td { renderSecond?.let { it() } }
                tr(classes = if (firstTd != secondTd) "diff" else null) {
                    th { +name.toString() }
                    unsafe { raw(firstTd) }
                    unsafe { raw(secondTd) }
                }
            }
        }
    }
}

fun comparison(
    name: Any,
    renderFirst: FlowContent.() -> Unit,
    renderSecond: (FlowContent.() -> Unit)? = null,
) = Triple(
    name,
    renderFirst,
    renderSecond,
)

fun comparison(
    name: Any,
    first: String,
    second: String? = null,
) = comparison(name, { +first }, { +second.orDash() })
