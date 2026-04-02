@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.*
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.vkt.VktSuoritusColumn
import fi.oph.kitu.vkt.VktSuoritusFilter
import fi.oph.kitu.vkt.VktSuoritusFlat
import fi.oph.kitu.vkt.VktSuoritusOrder
import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.h1
import kotlinx.html.h2

object VktKaikkiSuorituksetPage {
    fun render(
        suoritukset: Iterable<VktSuoritusFlat>,
        filter: VktSuoritusFilter,
        order: VktSuoritusOrder,
        pagination: Pagination,
        translations: Translations,
        messages: List<ViewMessageData>,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Valtionhallinnon kielitutkinto" }
            h2 { +"Kaikki suoritukset" }
            messages.forEach { viewMessage(it) }
            vktSearch(filter.search)
            article { +"Yhteensä: ${pagination.numberOfItems}" }
            vktKaikkiSuorituksetTable(suoritukset, filter, order, pagination, translations)
        }
}

fun FlowContent.vktKaikkiSuorituksetTable(
    suoritukset: Iterable<VktSuoritusFlat>,
    filter: VktSuoritusFilter,
    order: VktSuoritusOrder,
    pagination: Pagination,
    t: Translations,
) {
    card(overflowAuto = true, compact = true) {
        val columns =
            DisplayTableColumn.of<VktSuoritusColumn, VktSuoritusFlat>(
                setOf(ColumnTag.LIST_VIEW),
                emptySet(),
            )

        displayTable(
            suoritukset.toList(),
            columns,
            sortedBy = order.column,
            sortDirection = order.direction,
            testId = "ilmoittautuneet",
            rowTestId = { "${it.suorittajanOid}-${it.tutkintokieli}" },
            urlParams = mapOf("search" to filter.search),
        )
    }

    pagination(pagination)
}
