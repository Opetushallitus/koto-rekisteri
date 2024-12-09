package fi.oph.kitu.csvparsing.yki

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.exc.InvalidFormatException

class BooleanFromNumericDeserializer : JsonDeserializer<Boolean>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = when (p.text) {
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
