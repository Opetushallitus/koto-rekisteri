package fi.oph.kitu.csvparsing

import fi.oph.kitu.oid.Oid
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

class OidSerializer : ValueSerializer<Oid>() {
    override fun serialize(
        oid: Oid?,
        jsonGenerator: JsonGenerator?,
        serializationContext: SerializationContext,
    ) {
        jsonGenerator?.writeString(oid?.toString())
    }
}
