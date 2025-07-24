@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html

import fi.oph.kitu.HomeController
import fi.oph.kitu.html.Navigation.mainNavigation
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.hateoas.server.mvc.linkTo

object Page {
    fun HEAD.loadRaamit() {
        val isDeployEnvironment =
            AppConfig.environment.activeProfiles.any {
                it.lowercase().contains("untuva") ||
                    it.lowercase().contains("qa") ||
                    it.lowercase().contains("prod")
            }

        if (isDeployEnvironment) {
            script(
                type = "text/javascript",
                src = "https://${AppConfig.opintopolkuHostname}/virkailija-raamit/apply-raamit.js",
                crossorigin = null,
            ) {}
        }
    }

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
                link(href = "${AppConfig.appUrl}/pico.min.css", rel = "stylesheet")
                link(href = "${AppConfig.appUrl}/style.css", rel = "stylesheet")

                loadRaamit()
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
                                    li {
                                        a(
                                            href = linkTo(HomeController::home).toString(),
                                        ) { strong { +"Kielitutkintorekisteri" } }
                                    }
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
