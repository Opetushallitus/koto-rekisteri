package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.li
import kotlinx.html.ul

fun FlowContent.filterDescriptionList(descriptions: List<String>) {
    if (descriptions.isNotEmpty()) {
        ul {
            descriptions.forEach { li { +it } }
        }
    }
}

fun FlowContent.csvDownloadButton(href: String) {
    a(href = href) {
        attributes["download"] = ""
        +"Lataa tiedot CSV:nä"
    }
}
