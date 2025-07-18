@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html

import fi.oph.kitu.html.Navigation.mainNavigation
import jakarta.annotation.PostConstruct
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppConfig(
    @Value("\${kitu.opintopolkuHostname}")
    private val opintopolkuHostname: String,
) {
    @PostConstruct
    fun init() {
        Companion.opintopolkuHostname = opintopolkuHostname
    }

    companion object {
        lateinit var opintopolkuHostname: String private set
    }
}

object Page {
    fun renderHtml(
        breadcrumbs: List<Navigation.MenuItem>,
        wideContent: Boolean = false,
        renderBody: SECTION.() -> Unit,
    ): String {
        val pageTitle =
            listOf(
                "Kielitutkintorekisteri",
                breadcrumbs.joinToString(" - ") { it.title },
            ).filter { it.isNotEmpty() }
                .joinToString(" - ")

        return createHTML().html {
            lang = "fi"
            data("theme", "light")

            head {
                title { +pageTitle }
                meta(name = "color-scheme", content = "light")
                link(href = "/pico.min.css", rel = "stylesheet")
                link(href = "/style.css", rel = "stylesheet")
                script(
                    type = "text/javascript",
                    src = "https://${AppConfig.opintopolkuHostname}/virkailija-raamit/apply-raamit.js",
                    crossorigin = null,
                ) {}
            }
            body {
                testId("page-body")
                debugTrace()

                main {
                    nav(classes = "container-fluid main") {
                        testId("page-main-nav-header")
                        ul {
                            li {
                                ul(classes = "breadcrumbs") {
                                    testId("breadcrumbs")
                                    li { a(href = "/") { strong { +"Kielitutkintorekisteri" } } }
                                    breadcrumbs.forEach {
                                        li { a(href = it.ref) { +it.title } }
                                    }
                                }
                            }
                        }
                        ul {
                            testId("main-nav")
                            mainNavigation.forEach { group -> dropdown(group.name, group.children) }
                        }
                    }

                    section(classes = if (wideContent) "container-fluid" else "container") {
                        testId("page-content")
                        renderBody()
                    }
                }
            }
        }
    }
}
