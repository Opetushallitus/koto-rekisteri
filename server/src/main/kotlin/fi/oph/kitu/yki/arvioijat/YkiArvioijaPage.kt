package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Page
import fi.oph.kitu.html.displayTableHeader
import fi.oph.kitu.html.errorsArticle
import fi.oph.kitu.html.testId
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.i18n.finnishDateTimeUTC
import fi.oph.kitu.yki.YkiViewController
import kotlinx.html.article
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr
import org.springframework.hateoas.server.mvc.linkTo
import kotlin.String
import kotlin.enums.enumEntries

object YkiArvioijaPage {
    fun render(
        arvioijat: List<YkiArvioijaEntity>,
        sortColumn: YkiArvioijaColumn,
        sortDirection: SortDirection,
        errorsCount: Long,
    ): String =
        Page.renderHtml(
            wideContent = true,
        ) {
            h1 { +"Yleinen kielitutkinto" }
            h2 { +"Arvioijat" }
            this.errorsArticle(errorsCount, linkTo(YkiViewController::arvioijatVirheetView).toString())

            article(classes = "overflow-auto") {
                table(classes = "striped") {
                    displayTableHeader(
                        columns = enumEntries<YkiArvioijaColumn>().map { it.withValue(it.renderValue) },
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
                                            +arvioija.arvioijanOppijanumero.toString()
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
                                            arvioija.katuosoite?.let { +it }
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
                                            ?.finnishDateTimeUTC()
                                            ?.let { +it }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
