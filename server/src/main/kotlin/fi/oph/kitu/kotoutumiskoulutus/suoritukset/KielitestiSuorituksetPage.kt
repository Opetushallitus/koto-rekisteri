package fi.oph.kitu.kotoutumiskoulutus.suoritukset

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.errorsArticle
import fi.oph.kitu.html.hiddenValue
import fi.oph.kitu.html.hiddenValues
import fi.oph.kitu.html.input
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableColumn
import fi.oph.kitu.html.table.dateFilter
import fi.oph.kitu.html.table.displayTableBody
import fi.oph.kitu.html.table.displayTableHeader
import fi.oph.kitu.html.table.enumFilter
import fi.oph.kitu.html.table.httpParams
import fi.oph.kitu.html.table.tableFilterDialog
import fi.oph.kitu.html.table.toggleFilter
import fi.oph.kitu.html.testId
import fi.oph.kitu.kotoutumiskoulutus.KielitestiApiController
import fi.oph.kitu.kotoutumiskoulutus.KielitestiViewController
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.YkiApiController
import fi.oph.kitu.yki.YkiSuorituksetParams
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.button
import kotlinx.html.fieldSet
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.header
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.table
import kotlinx.html.ul
import org.springframework.hateoas.server.mvc.linkTo

object KielitestiSuorituksetPage {
    fun render(
        suoritukset: List<KielitestiSuoritus>,
        errorsCount: Long,
        filterParams: KielitestiSuorituksetParams,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Kotoutumiskoulutuksen kielitaidon päättötesti" }
            h2 { +"Suoritukset" }
            errorsArticle(errorsCount, linkTo(KielitestiViewController::virheetView).toString())

            form(action = "", method = FormMethod.get, classes = "grid center-vertically") {
                hiddenValues(filterParams.toMap().filterKeys { it != "search" })
                fieldSet {
                    attributes["role"] = "search"
                    input {
                        testId("search")
                        id = "search"
                        type = InputType.search
                        name = "search"
                        value = filterParams.search
                        placeholder = "Oppijanumero, nimi tai muu hakusana"
                    }
                    button {
                        testId("search-button")
                        type = ButtonType.submit
                        +"Suodata"
                    }
                }
            }

            article(classes = "overflow-auto") {
                header {
                    nav {
                        ul {
                            li {
                                +"Suorituksia yhteensä: ${suoritukset.size}"
                            }
                            li { kielitestiCsvDownloadButton(filterParams) }
                            li { kielitestiSuoritusFilterButton(filterParams) }
                        }
                    }
                    kielitestiSuoritusFilterList(filterParams)
                }

                table {
                    val columns =
                        DisplayTableColumn.of<KielitestiSuoritusColumn, KielitestiSuoritus>(
                            setOf(ColumnTag.LIST_VIEW),
                            filterParams.excludeTags(),
                        )
                    displayTableHeader(
                        columns = columns,
                        sortedBy = filterParams.sortColumn,
                        sortDirection = filterParams.sortDirection,
                        preserveSortDirection = false,
                        selectableRows = false,
                        tableId = "kielitesti-suoritukset-table",
                        urlParams = mapOf("search" to filterParams.search),
                    )
                    displayTableBody(
                        rows = suoritukset,
                        columns = columns,
                        rowClasses = "suoritus",
                        rowTestId = { "suoritus-summary-row" },
                    )
                }
            }
        }
}

fun FlowContent.kielitestiSuoritusFilterButton(params: KielitestiSuorituksetParams) {
    tableFilterDialog("suoritukset") {
        hiddenValue("search", params.search)
        fieldSet(classes = "grid") {
            dateFilter("suoritusalku", "Suoritusaika alkaen", params.suoritusalku)
            dateFilter("suoritusloppu", "Suoritusaika päättyen", params.suoritusloppu)
        }
        fieldSet {
            enumFilter<Testikieli>("testikieli", "Testikieli", params.testikieli)
        }
        fieldSet {
            toggleFilter("piilotaHenkilotiedot", "Piilota henkilötiedot", params.piilotaHenkilotiedot)
        }
    }
}

fun FlowContent.kielitestiSuoritusFilterList(params: KielitestiSuorituksetParams) {
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

fun FlowContent.kielitestiCsvDownloadButton(params: KielitestiSuorituksetParams) {
    a(
        href =
            linkTo<KielitestiApiController> {
                getSuorituksetAsCsv()
            }.toString() + "?${httpParams(params.toMap())}",
    ) {
        attributes["download"] = ""
        +"Lataa tiedot CSV:nä"
    }
}
