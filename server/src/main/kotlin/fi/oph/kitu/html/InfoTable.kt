package fi.oph.kitu.html

import fi.oph.kitu.i18n.LocalizedString
import kotlinx.html.FlowContent
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr

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
