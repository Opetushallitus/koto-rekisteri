package fi.oph.kitu.csvparsing

import fi.oph.kitu.oid.Oid
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class OidDeserializer : ValueDeserializer<Oid>() {
    override fun deserialize(
        parser: JsonParser,
        context: DeserializationContext,
    ): Oid? =
        parser.valueAsString?.let {
            Oid.parse(it).getOrNull()
        }
}
