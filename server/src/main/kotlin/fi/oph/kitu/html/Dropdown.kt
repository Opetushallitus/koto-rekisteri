package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.details
import kotlinx.html.li
import kotlinx.html.strong
import kotlinx.html.summary
import kotlinx.html.ul

fun FlowContent.dropdown(
    title: String,
    items: List<Navigation.MenuItem>,
) {
    ul {
        testId("main-nav")
        li {
            details(classes = "dropdown") {
                summary {
                    +title
                }
                ul {
                    testId("dropdown-menu")
                    items.forEach {
                        li {
                            if (it.ref == null) {
                                strong { +it.title }
                            } else {
                                a(href = it.ref) { +it.title }
                            }
                        }
                    }
                }
            }
        }
    }
}
