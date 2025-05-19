package fi.oph.kitu.html

import kotlinx.html.FlowContent

data class MenuItem(
    val title: String,
    val href: String,
)

fun FlowContent.data(
    key: String,
    value: String,
) {
    attributes["data-$key"] = value
}

fun FlowContent.testId(id: String) {
    data("testid", id)
}
