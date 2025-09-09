@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import kotlinx.html.*

fun FlowContent.json(node: JsonNode) {
    when (node) {
        is ArrayNode -> ul { node.forEach { li { json(it) } } }
        is ObjectNode ->
            ul {
                node.properties().forEach { prop ->
                    li {
                        b {
                            +prop.key
                            +": "
                        }
                        span { json(prop.value) }
                    }
                }
            }
        is TextNode -> +node.textValue()
        is NumericNode -> +node.asText()
        else -> +node.toString()
    }
}
