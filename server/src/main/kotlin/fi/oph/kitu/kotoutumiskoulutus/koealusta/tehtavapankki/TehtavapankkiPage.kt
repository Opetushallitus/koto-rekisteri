package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.i18n.finnishDateTimeUTC
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TehtavapankkiPage {
    fun render(tehtavapaketit: List<TehtavapakettiObject>): String =
        Page.renderHtml {
            h1 { +"Kotoutumiskoulutuksen tehtäväpankki" }
            h2 { +"Tehtäväpaketit" }

            if (tehtavapaketit.isEmpty()) {
                p { +"Ei tehtäväpaketteja." }
            } else {
                card(overflowAuto = true) {
                    table(classes = "compact striped") {
                        thead {
                            tr {
                                th { +"Avain" }
                                th { +"Aikaleima" }
                                th { +"Lataa" }
                            }
                        }
                        tbody {
                            tehtavapaketit.forEach { tp ->
                                tr {
                                    td { +tp.key }
                                    td { +tp.timestamp.finnishDateTimeUTC() }
                                    td {
                                        a(
                                            href =
                                                linkTo(
                                                    methodOn(TehtavapankkiViewController::class.java)
                                                        .downloadRedirect(tp.key),
                                                ).toString(),
                                        ) {
                                            attributes["download"] = ""
                                            +"Lataa"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
