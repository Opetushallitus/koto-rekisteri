@file:Suppress("ktlint:standard:no-wildcard-imports")

package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.b
import kotlinx.html.li
import kotlinx.html.span
import kotlinx.html.ul
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.NumericNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

fun FlowContent.json(node: JsonNode) {
    when (node) {
        is ArrayNode -> {
            ul { node.forEach { li { json(it) } } }
        }

        is ObjectNode -> {
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
        }

        is StringNode -> {
            +node.asString()
        }

        is NumericNode -> {
            +node.asString()
        }

        else -> {
            +node.toString()
        }
    }
}
