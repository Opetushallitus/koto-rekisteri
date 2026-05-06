package fi.oph.kitu.vkt.html

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.html.ViewMessageData
import fi.oph.kitu.html.card
import fi.oph.kitu.html.csvDownloadButton
import fi.oph.kitu.html.filterDescriptionList
import fi.oph.kitu.html.hiddenValue
import fi.oph.kitu.html.pagination
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.dateFilter
import fi.oph.kitu.html.table.displayTable
import fi.oph.kitu.html.table.enumFilter
import fi.oph.kitu.html.table.httpParams
import fi.oph.kitu.html.table.tableFilterDialog
import fi.oph.kitu.html.table.toggleFilter
import fi.oph.kitu.html.table.trueFalseOrAllFilter
import fi.oph.kitu.html.testId
import fi.oph.kitu.html.viewMessage
import fi.oph.kitu.i18n.Translations
import fi.oph.kitu.vkt.VktApiController
import fi.oph.kitu.vkt.VktSuoritusColumn
import fi.oph.kitu.vkt.VktSuoritusFilter
import fi.oph.kitu.vkt.VktSuoritusFlat
import fi.oph.kitu.vkt.VktSuoritusOrder
import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.fieldSet
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.header
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.span
import kotlinx.html.ul
import org.springframework.hateoas.server.mvc.linkTo

object VktSuorituksetPage {
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
            vktSearch(filter)
            article {
                header {
                    nav {
                        ul {
                            li {
                                +"Yhteensä: "
                                span {
                                    testId("numberOfRows")
                                    +pagination.numberOfItems.toString()
                                }
                            }
                            li {
                                csvDownloadButton(
                                    linkTo<VktApiController> { getSuorituksetCsv() }.toString() +
                                        httpParams(filter.toMap()),
                                )
                            }
                            li { vktSuoritusFilterButton(filter) }
                        }
                    }
                }
                filterDescriptionList(filter.filterDescriptions())
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
                filter.excludeTags(),
            )

        displayTable(
            suoritukset.toList(),
            columns,
            sortedBy = order.sortColumn,
            sortDirection = order.sortDirection,
            testId = "suoritukset",
            rowTestId = { "${it.suorittajanOid}-${it.tutkintokieli}" },
            urlParams = filter.toMap(),
        )
    }

    pagination(pagination)
}

fun FlowContent.vktSuoritusFilterButton(filter: VktSuoritusFilter) {
    tableFilterDialog("") {
        filter.search?.let { hiddenValue("search", filter.search) }
        fieldSet(classes = "grid") {
            dateFilter("alkupaiva", "Alkaen", filter.alkupaiva)
            dateFilter("loppupaiva", "Päättyen", filter.loppupaiva)
        }
        fieldSet {
            enumFilter("tutkintokieli", "Tutkintokieli", filter.tutkintokieli)
        }
        fieldSet {
            enumFilter("taitotaso", "Taitotaso", filter.taitotaso)
        }
        fieldSet {
            enumFilter(
                "arvioitu",
                "Erinomaisen tason suoritusten arvioinnin tila",
                filter.arvioitu,
            )
        }
        fieldSet {
            trueFalseOrAllFilter(
                "merkittyPoistettavaksi",
                "Poistettavaksi merkitty erinomaisen tason suoritus",
                filter.merkittyPoistettavaksi,
                Triple("Näytä kaikki", "Näytä vain poistettavat suoritukset", "Piilota poistettavat suoritukset"),
            )
        }
        fieldSet {
            toggleFilter("piilotaHenkilotiedot", "Piilota henkilötiedot", filter.piilotaHenkilotiedot)
        }
    }
}
