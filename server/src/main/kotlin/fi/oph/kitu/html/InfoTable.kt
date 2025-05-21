package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr

fun FlowContent.infoTable(vararg rows: Pair<String, FlowContent.() -> Unit>) {
    table(classes = "info-table compact striped") {
        debugTrace()
        tbody {
            rows.forEach { (name, render) ->
                tr {
                    th { +name }
                    td { render() }
                }
            }
        }
    }
}
