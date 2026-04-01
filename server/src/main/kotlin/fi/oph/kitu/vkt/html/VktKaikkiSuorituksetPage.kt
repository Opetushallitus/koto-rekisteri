@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.*
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritusColumn
import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.h1
import kotlinx.html.h2

object VktKaikkiSuorituksetPage {
    fun render(
        suoritukset: List<VktTableItem>,
        sortedBy: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        pagination: Pagination,
        translations: Translations,
        searchQuery: String?,
        messages: List<ViewMessageData>,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Valtionhallinnon kielitutkinto" }
            h2 { +"Kaikki suoritukset" }
            messages.forEach { viewMessage(it) }
            vktSearch(searchQuery)
            article { +"Yhteensä: ${pagination.numberOfItems}" }
            vktKaikkiSuorituksetTable(suoritukset, sortedBy, sortDirection, pagination, translations, searchQuery)
        }
}

fun FlowContent.vktKaikkiSuorituksetTable(
    suoritukset: List<VktTableItem>,
    sortedBy: CustomVktSuoritusRepository.Column,
    sortDirection: SortDirection,
    pagination: Pagination,
    t: Translations,
    searchQuery: String?,
) {
    card(overflowAuto = true, compact = true) {
        val columns =
            DisplayTableColumn.of<VktSuoritusColumn, VktTableItem>(
                setOf(ColumnTag.LIST_VIEW),
                emptySet(),
            )

        displayTable(
            suoritukset,
            columns,
            sortedBy = sortedBy,
            sortDirection = sortDirection,
            testId = "ilmoittautuneet",
            rowTestId = { "${it.oppijanumero}-${it.kieli.koodiarvo}" },
            urlParams = mapOf("search" to searchQuery),
        )
    }

    pagination(pagination)
}
