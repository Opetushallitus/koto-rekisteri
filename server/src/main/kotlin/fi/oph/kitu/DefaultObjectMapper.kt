package fi.oph.kitu

import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.StringNode
import tools.jackson.module.kotlin.kotlinModule

/**
 * Yleiskäyttöinen json-mapper, jolle on konffattu:
 *
 *      - Tuki ISO-aikaleimoille (Jackson 3 tukee näitä oletuksena)
 *      - Tuki Kotlin-tietotyypeille
 *      - Nätti JSON-tulostus
 *      - Ei välitä yllättävistä propertyista
 */
val defaultObjectMapper by lazy {
    JsonMapper
        .builder()
        .addModule(kotlinModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
}

fun String.toJsonNode(): JsonNode =
    try {
        defaultObjectMapper.readTree(this)
    } catch (_: JacksonException) {
        StringNode(this)
    }
