package fi.oph.kitu.webmvc

import fi.oph.kitu.html.Page
import fi.oph.kitu.html.card
import fi.oph.kitu.html.cardContent
import fi.oph.kitu.html.classes
import fi.oph.kitu.html.testId
import fi.oph.kitu.i18n.formatRelativeTime
import kotlinx.html.FlowContent
import kotlinx.html.UL
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.span
import kotlinx.html.ul
import java.time.Instant

object HomePage {
    fun render(stats: DashboardStats): String =
        Page.renderHtml {
            h1 { +"Kielitutkintorekisteri" }
            div(classes = "grid dashboard-grid") {
                testId("dashboard")
                ykiCard(stats.yki)
                vktCard(stats.vkt)
                kotoCard(stats.koto)
                adminCard()
            }
        }

    private fun FlowContent.ykiCard(s: YkiStats) =
        sectionCard(
            groupId = "yki",
            title = "Yleinen kielitutkinto",
        ) {
            statRow("Suoritukset", s.suoritusCount, Links.Yki.suoritukset())
            statRow("Arvioijat", s.arvioijaCount, Links.Yki.arvioijat())
            statRow("Tarkistusarvioinnit", value = null, href = Links.Yki.tarkistusArvioinnit())
            statRow(
                label = "Suoritusten tuonnin virheet",
                value = s.suoritusImportErrorCount,
                href = Links.Yki.suorituksetVirheet(),
                errorIfNonZero = true,
            )
            statRow(
                label = "Arvioijien tuonnin virheet",
                value = s.arvioijaImportErrorCount,
                href = Links.Yki.arvioijatVirheet(),
                errorIfNonZero = true,
            )
            statRow(
                label = "Koski-siirron virheet",
                value = s.koskiErrorCount,
                href = Links.Yki.koskiVirheet(),
                errorIfNonZero = true,
            )
            statRow(
                label = "Poikkeamat",
                value = s.poikkeamatCount,
                href = Links.Yki.poikkeamat(),
                warnIfNonZero = true,
            )
            latestReceivedRow(s.latestReceivedAt, Links.Yki.suoritukset())
        }

    private fun FlowContent.vktCard(s: VktStats) =
        sectionCard(
            groupId = "vkt",
            title = "Valtionhallinnon kielitutkinto",
        ) {
            statRow("Kaikki suoritukset", s.suoritusCount, Links.Vkt.suoritukset())
            statRow(
                label = "Erinomaisen taidon ilmoittautuneet",
                value = s.ilmoittautuneetErinomaisenTaso,
                href = Links.Vkt.erinomaisenTaitotasonIlmoittautuneet(),
            )
            statRow(
                label = "Erinomaisen taidon suoritukset",
                value = s.suorituksetErinomaisenTaso,
                href = Links.Vkt.erinomaisenTaitotasonArvioidutSuoritukset(),
            )
            statRow(
                label = "Hyvän ja tyydyttävän taidon suoritukset",
                value = s.suorituksetHyvaJaTyydyttavaTaso,
                href = Links.Vkt.hyvanJaTyydyttavanTaitotasonSuoritukset(),
            )
            statRow(
                label = "Koski-siirron virheet",
                value = s.koskiErrorCount,
                href = Links.Vkt.koskiVirheet(),
                errorIfNonZero = true,
            )
            latestReceivedRow(s.latestReceivedAt, Links.Vkt.suoritukset())
        }

    private fun FlowContent.kotoCard(s: KotoStats) =
        sectionCard(
            groupId = "koto-kielitesti",
            title = "Kotoutumiskoulutuksen kielitaidon päättötesti",
        ) {
            statRow("Suoritukset", s.suoritusCount, Links.Kielitesti.suoritukset())
            statRow("Tehtäväpaketit", value = null, href = Links.Tehtavapankki.list())
            statRow(
                label = "Tuonnin virheet",
                value = s.importErrorCount,
                href = Links.Kielitesti.virheet(),
                errorIfNonZero = true,
            )
            latestReceivedRow(s.latestReceivedAt, Links.Kielitesti.suoritukset())
        }

    private fun FlowContent.adminCard() =
        sectionCard(
            groupId = "admin",
            title = "Ylläpito",
        ) {
            statRow("Eräajojen hallinta", value = null, href = "/kielitutkinnot/db-scheduler")
        }

    private fun FlowContent.sectionCard(
        groupId: String,
        title: String,
        rows: UL.() -> Unit,
    ) = card {
        testId("$groupId-links")
        cardContent {
            h2(classes = "dashboard-card-title") { +title }
            ul(classes = "dashboard-stats") { rows() }
        }
    }

    private fun UL.statRow(
        label: String,
        value: Long?,
        href: String,
        errorIfNonZero: Boolean = false,
        warnIfNonZero: Boolean = false,
    ) {
        val nonZero = value != null && value > 0L
        val badgeClass =
            when {
                !nonZero -> null
                errorIfNonZero -> "badge badge-error"
                warnIfNonZero -> "badge badge-warning"
                else -> null
            }
        li(classes = "stat-row") {
            a(href = href, classes = "stat-link") {
                span(classes = "stat-label") { +label }
                span(classes = classes(true to "stat-value", (badgeClass != null) to (badgeClass ?: ""))) {
                    +(value?.toString() ?: "→")
                }
            }
        }
    }

    private fun UL.latestReceivedRow(
        latestReceivedAt: Instant?,
        href: String,
    ) = li(classes = "stat-row") {
        a(href = href, classes = "stat-link") {
            span(classes = "stat-label") { +"Viimeisin saapunut suoritus" }
            span(classes = "stat-value stat-value-muted") {
                testId("latest-received")
                +formatRelativeTime(latestReceivedAt)
            }
        }
    }
}
