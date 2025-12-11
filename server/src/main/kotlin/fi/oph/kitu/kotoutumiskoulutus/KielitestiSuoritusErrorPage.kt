package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.Oid
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTableBody
import fi.oph.kitu.html.displayTableHeader
import fi.oph.kitu.i18n.LocalizedString
import kotlinx.html.article
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.table
import kotlin.enums.enumEntries

object KielitestiSuoritusErrorPage {
    fun render(
        sortColumn: KielitestiSuoritusErrorColumn,
        sortDirection: SortDirection,
        errors: Iterable<KielitestiSuoritusError>,
        organisaatioidenNimet: Map<Oid, LocalizedString>,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Kotoutumiskoulutuksen kielitaidon päättötesti" }
            h2 { +"Suoritusten tuonnin virheet" }
            article(classes = "overflow-auto") {
                table(classes = "compact striped") {
                    val columns =
                        enumEntries<KielitestiSuoritusErrorColumn>().map {
                            it.withValue(it.renderValue(organisaatioidenNimet))
                        }

                    displayTableHeader(
                        columns = columns,
                        sortedBy = sortColumn,
                        sortDirection = sortDirection,
                        preserveSortDirection = false,
                        selectableRows = false,
                        tableId = "kielitesti-suoritukset-virheet-table",
                    )

                    displayTableBody(
                        rows = errors.toList(),
                        columns = columns,
                        tbodyClasses = "virheet",
                        rowTestId = { "virhe-summary-row" },
                    )
                }
            }
        }
}
