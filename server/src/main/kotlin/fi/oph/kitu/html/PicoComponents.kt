package fi.oph.kitu.html

import kotlinx.html.ARTICLE
import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.div
import kotlinx.html.option
import kotlinx.html.section
import kotlinx.html.select

// https://picocss.com/docs/card
fun FlowContent.card(
    overflowAuto: Boolean = false,
    compact: Boolean = false,
    content: ARTICLE.() -> Unit,
) {
    article(classes = classes(overflowAuto to "overflow-auto", compact to "compact")) {
        content()
    }
}

fun FlowContent.cardContent(block: FlowContent.() -> Unit) {
    section(classes = "cardContent") {
        block()
    }
}

// https://picocss.com/docs/forms/select
fun FlowContent.itemSelect(
    inputName: String,
    items: List<Navigation.MenuItem>,
    includeBlank: Boolean = false,
    testId: String? = null,
) {
    select {
        name = inputName
        testId(testId)
        if (includeBlank) {
            option {}
        }
        items.forEach {
            option {
                value = it.ref
                selected = it.current
                +it.title
            }
        }
    }
}
