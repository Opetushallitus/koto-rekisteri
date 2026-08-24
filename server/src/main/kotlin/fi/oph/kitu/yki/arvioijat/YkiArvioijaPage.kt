package fi.oph.kitu.yki.arvioijat
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.table.displayTableHeader
import fi.oph.kitu.html.testId
import fi.oph.kitu.i18n.UiText
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTime
import fi.oph.kitu.i18n.unaryPlus
import fi.oph.kitu.jdbc.SortDirection
import kotlinx.html.article
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr
import kotlin.String
import kotlin.enums.enumEntries

object YkiArvioijaPage {
    fun render(
        arvioijat: List<YkiArvioijaEntity>,
        sortColumn: YkiArvioijaColumn,
        sortDirection: SortDirection,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +UiText.Nav.yki }
            h2 { +UiText.Nav.arvioijat }

            article(classes = "overflow-auto") {
                table(classes = "striped") {
                    displayTableHeader(
                        columns = enumEntries<YkiArvioijaColumn>().map { it.withValue(it.getValue) },
                        sortedBy = sortColumn,
                        sortDirection = sortDirection,
                        preserveSortDirection = true,
                        selectableRows = false,
                        tableId = "arvioijat-table",
                    )
                    tbody {
                        arvioijat.forEach { arvioija ->
                            val rowSpan = arvioija.arviointioikeudet.size.toString()
                            arvioija.arviointioikeudet.forEachIndexed { i, ao ->
                                tr {
                                    if (i == 0) {
                                        td {
                                            attributes["rowspan"] = rowSpan
                                            testId("")
                                            +arvioija.arvioijaOid.toString()
                                        }
                                        td {
                                            attributes["rowspan"] = rowSpan
                                            arvioija.henkilotunnus?.let { +it }
                                        }
                                        td {
                                            attributes["rowspan"] = rowSpan
                                            testId("sukunimi")
                                            +arvioija.sukunimi
                                        }
                                        td {
                                            attributes["rowspan"] = rowSpan
                                            +arvioija.etunimet
                                        }
                                        td {
                                            attributes["rowspan"] = rowSpan
                                            arvioija.sahkopostiosoite?.let { +it }
                                        }
                                        td {
                                            attributes["rowspan"] = rowSpan
                                            +arvioija.katuosoite
                                        }
                                    }
                                    td { +ao.tila.name }
                                    td { +ao.kieli.solkiCode }
                                    td { +ao.tasot.joinToString(", ") { it.name } }
                                    td { ao.kaudenAlkupaiva?.let { finnishDate(it) } }
                                    td { ao.kaudenPaattymispaiva?.let { finnishDate(it) } }
                                    td { +ao.jatkorekisterointi.toString() }
                                    td {
                                        ao.rekisteriintuontiaika
                                            ?.toInstant()
                                            ?.let { finnishDateTime(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
