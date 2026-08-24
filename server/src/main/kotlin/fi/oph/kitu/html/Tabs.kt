package fi.oph.kitu.html

import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.unaryPlus
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.ul

data class TabItem(
    val label: LocalizedString,
    val href: String,
    val selected: Boolean,
    val testId: String? = null,
)

fun FlowContent.tabs(vararg items: TabItem) {
    nav(classes = "tabs") {
        ul {
            items.forEach { item ->
                li {
                    a(href = item.href) {
                        if (item.selected) attributes["aria-current"] = "page"
                        testId(item.testId)
                        +item.label
                    }
                }
            }
        }
    }
}
