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
import kotlin.math.log10
import kotlin.math.pow

object TehtavapankkiPage {
    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return "%.2f %s".format(value, units[digitGroups])
    }

    fun render(
        tehtavapaketit: Map<String, List<TehtavapakettiObject>>,
        pakettiIdsByS3Avain: Map<String, Int> = emptyMap(),
    ): String =
        Page.renderHtml {
            h1 { +"Kotoutumiskoulutuksen tehtäväpankki" }

            if (tehtavapaketit.isEmpty()) {
                p { +"Ei tehtäväpaketteja." }
            } else {
                tehtavapaketit.forEach { (group, tps) ->
                    h2 { +group }
                    card(overflowAuto = true, compact = true) {
                        table(classes = "compact striped") {
                            thead {
                                tr {
                                    th { +"Siirretty" }
                                    th { +"Koko" }
                                    th { +"Sisältö" }
                                }
                            }
                            tbody {
                                tps.forEachIndexed { index, tp ->
                                    tr(classes = if (index > 0) "faded" else null) {
                                        td { +tp.timestamp.finnishDateTimeUTC() }
                                        td { +formatBytes(tp.size) }
                                        td {
                                            val pakettiId = pakettiIdsByS3Avain[tp.key]
                                            if (pakettiId != null) {
                                                // Paketti löytyy DB:stä — linkki näkymään, josta voi myös ladata XML:n.
                                                a(
                                                    href =
                                                        linkTo(
                                                            methodOn(TehtavapankkiViewController::class.java)
                                                                .pakettiView(pakettiId),
                                                        ).toString(),
                                                ) { +"Näytä sisältö" }
                                            } else {
                                                // Ei selattavaa versiota — tarjotaan raaka XML.
                                                a(
                                                    href =
                                                        linkTo(
                                                            methodOn(TehtavapankkiViewController::class.java)
                                                                .downloadRedirect(tp.key),
                                                        ).toString(),
                                                ) {
                                                    attributes["download"] = ""
                                                    +"Lataa XML"
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
        }
}
