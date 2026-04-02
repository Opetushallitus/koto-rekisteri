@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.*
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.dateFilter
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.html.table.enumFilter
import fi.oph.kitu.html.table.httpParams
import fi.oph.kitu.html.table.tableFilterDialog
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.vkt.VktApiController
import fi.oph.kitu.vkt.VktSuoritusColumn
import fi.oph.kitu.vkt.VktSuoritusFilter
import fi.oph.kitu.vkt.VktSuoritusFlat
import fi.oph.kitu.vkt.VktSuoritusOrder
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.fieldSet
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.header
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.ul
import org.springframework.hateoas.server.mvc.linkTo

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
            article {
                header {
                    nav {
                        ul {
                            li { +"Yhteensä: ${pagination.numberOfItems}" }
                            li { csvDownloadButton(filter) }
                            li { vktSuoritusFilterButton(filter) }
                        }
                    }
                }
                vktSuoritusFilterList(filter)
            }
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
            sortedBy = order.sortColumn,
            sortDirection = order.sortDirection,
            testId = "ilmoittautuneet",
            rowTestId = { "${it.suorittajanOid}-${it.tutkintokieli}" },
            urlParams = filter.toMap(),
        )
    }

    pagination(pagination)
}

fun FlowContent.vktSuoritusFilterButton(params: VktSuoritusFilter) {
    tableFilterDialog("") {
        fieldSet(classes = "grid") {
            dateFilter("alkupaiva", "Alkaen", params.alkupaiva)
            dateFilter("loppupaiva", "Päättyen", params.loppupaiva)
        }
        fieldSet {
            enumFilter("tutkintokieli", "Tutkintokieli", params.tutkintokieli)
        }
        fieldSet {
            enumFilter("taitotaso", "Taitotaso", params.taitotaso)
        }
//        fieldSet {
//            toggleFilter("piilotaHenkilotiedot", "Piilota henkilötiedot", params.piilotaHenkilotiedot)
//        }
//        fieldSet {
//            toggleFilter(
//                "piilotaVanhentuneetTiedot",
//                "Piilota vanhentuneet tietokentät",
//                params.piilotaVanhentuneetTiedot,
//            )
//        }
    }
}

fun FlowContent.csvDownloadButton(params: VktSuoritusFilter) {
    a(
        href =
            linkTo<VktApiController> {
                getSuorituksetCsv()
            }.toString() + "?${httpParams(params.toMap())}",
    ) {
        attributes["download"] = ""
        +"Lataa tiedot CSV:nä"
    }
}

fun FlowContent.vktSuoritusFilterList(params: VktSuoritusFilter) {
    params.filterDescriptions().let { filters ->
        if (filters.isNotEmpty()) {
            ul {
                filters.forEach { filter ->
                    li { +filter }
                }
            }
        }
    }
}
