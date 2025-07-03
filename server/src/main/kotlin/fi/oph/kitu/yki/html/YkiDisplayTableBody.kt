package fi.oph.kitu.yki.html

import fi.oph.kitu.html.DisplayTableColumn
import fi.oph.kitu.html.testId
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import kotlinx.html.TABLE
import kotlinx.html.TBODY
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr
import kotlin.collections.forEach

fun TABLE.ykiDisplayTableBody(
    rows: List<YkiSuoritusEntity>,
    columns: List<DisplayTableColumn<YkiSuoritusEntity>>,
    rowTestId: ((YkiSuoritusEntity) -> String)? = null,
    tbodyClasses: String? = null,
    afterRow: TBODY.(YkiSuoritusEntity) -> Unit,
) {
    rows.forEach { row ->
        tbody(classes = tbodyClasses) {
            tr {
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
