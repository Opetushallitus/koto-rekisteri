@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html

import fi.oph.kitu.config.ApplicationProperties
import fi.oph.kitu.html.Navigation.flatten
import fi.oph.kitu.html.Navigation.mainNavigation
import fi.oph.kitu.webmvc.HomeController
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.hateoas.server.mvc.linkTo

object Page {
    fun HEAD.loadRaamit() {
        val isDeployEnvironment =
            ApplicationProperties.environment.activeProfiles.any {
                it.lowercase().contains("untuva") ||
                    it.lowercase().contains("qa") ||
                    it.lowercase().contains("prod")
            }

        if (isDeployEnvironment) {
            script(
                type = "text/javascript",
                src = "https://${ApplicationProperties.kitu.opintopolkuHostname}/virkailija-raamit/apply-raamit.js",
                crossorigin = null,
            ) {}
        }
    }

    fun renderHtml(
        wideContent: Boolean = false,
        renderBody: SECTION.() -> Unit,
    ): String {
        val pageTitle = "Kielitutkintorekisteri"

        return createHTML().html {
            lang = "fi"
            data("theme", "light")

            head {
                title { +pageTitle }
                meta(name = "color-scheme", content = "light")
                link(href = "${ApplicationProperties.kitu.appUrl}/pico.min.css", rel = "stylesheet")
                link(href = "${ApplicationProperties.kitu.appUrl}/style.css", rel = "stylesheet")

                loadRaamit()
            }
            body {
                testId("page-body")
                debugTrace()

                main {
                    nav(classes = "container-fluid main") {
                        testId("page-main-nav-header")
                        ul(classes = "nav-title") {
                            li {
                                ul(classes = "breadcrumbs") {
                                    testId("breadcrumbs")
                                    li {
                                        a(
                                            href = linkTo(HomeController::home).toString(),
                                        ) { strong { +"Kielitutkintorekisteri" } }
                                    }
                                }
                            }
                        }
                        ul(classes = "nav-menu") {
                            testId("main-nav")
                            dropdown(
                                "☰",
                                mainNavigation.flatten(),
                            )
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
