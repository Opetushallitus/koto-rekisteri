package fi.oph.kitu.html

import kotlinx.html.ARTICLE
import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.option
import kotlinx.html.select

// https://picocss.com/docs/card
fun FlowContent.card(
    overflowAuto: Boolean = false,
    content: ARTICLE.() -> Unit,
) {
    article(classes = if (overflowAuto) "overflow-auto" else null) {
        content()
    }
}

// https://picocss.com/docs/forms/select
fun FlowContent.itemSelect(
    inputName: String,
    items: List<MenuItem>,
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
