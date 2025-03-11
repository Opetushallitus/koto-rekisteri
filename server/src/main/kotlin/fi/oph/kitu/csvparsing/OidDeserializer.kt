package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import fi.oph.kitu.Oid

class OidDeserializer : JsonDeserializer<Oid>() {
    override fun deserialize(
        parser: JsonParser?,
        context: DeserializationContext?,
    ): Oid? =
        parser?.valueAsString?.let {
            Oid.parse(it).getOrNull()
        }
}
