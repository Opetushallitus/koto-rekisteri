package fi.oph.kitu.csvparsing

import fi.oph.kitu.Oid
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class OidDeserializer : StdDeserializer<Oid>(Oid::class.java) {
    override fun deserialize(
        p: JsonParser?,
        ctxt: DeserializationContext?,
    ): Oid? =
        p?.valueAsString?.let {
            Oid.parse(it).getOrNull()
        }
}
