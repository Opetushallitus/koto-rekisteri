@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML

object Page {
    fun renderHtml(
        breadcrumbs: List<MenuItem>,
        renderBody: SECTION.() -> Unit,
    ): String {
        val pageTitle = "Kielitutkintorekisteri - " + breadcrumbs.joinToString(" - ") { it.title }

        return createHTML().html {
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
                        ul {
                            li {
                                ul(classes = "breadcrumbs") {
                                    testId("breadcrumbs")
                                    li { a(href = "/") { strong { +"Kielitutkintorekisteri" } } }
                                    breadcrumbs.forEach {
                                        li { a(href = it.href) { +it.title } }
                                    }
                                }
                            }
                        }
                        ul {
                            testId("main-nav")
                            mainNavigation.forEach { (title, items) -> dropdown(title, items) }
                        }
                    }

                    section(classes = "container-fluid") {
                        testId("page-content")
                        renderBody()
                    }
                }
            }
        }
    }

    val mainNavigation =
        mapOf(
            "Yleiset kielitutkinnot" to
                listOf(
                    MenuItem("Suoritukset", "/yki/suoritukset"),
                    MenuItem("Arvioijat", "/yki/arvioijat"),
                ),
            "Kotoutumiskoulutuksen kielikokeet" to
                listOf(
                    MenuItem("Suoritukset", "/koto-kielitesti/suoritukset"),
                ),
            "Valtionhallinnon kielitutkinto" to
                listOf(
                    MenuItem("Erinomaisen tason ilmoittautuneet", "/vkt/ilmoittautuneet"),
                ),
        )
}
