@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html

import fi.oph.kitu.html.Navigation.mainNavigation
import kotlinx.html.*
import kotlinx.html.stream.createHTML

object Page {
    fun renderHtml(
        breadcrumbs: List<Navigation.MenuItem>,
        wideContent: Boolean = false,
        renderBody: SECTION.() -> Unit,
    ): String {
        val pageTitle = "Kielitutkintorekisteri - " + breadcrumbs.joinToString(" - ") { it.title }

        return createHTML().html {
            lang = "fi"

            head {
                title { +pageTitle }
                link(href = "/pico.min.css", rel = "stylesheet")
                link(href = "/style.css", rel = "stylesheet")
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
