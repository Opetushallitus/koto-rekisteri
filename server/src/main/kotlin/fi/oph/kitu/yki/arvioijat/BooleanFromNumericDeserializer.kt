package fi.oph.kitu.yki.arvioijat

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.exc.InvalidFormatException

class BooleanFromNumericDeserializer : ValueDeserializer<Any>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Boolean =
        when (p.text) {
            "0" -> false

            "1" -> true

            else -> throw InvalidFormatException(
                p,
                "Expected '0' or '1' for Boolean field",
                p.text,
                Boolean::class.java,
            )
        }
}
