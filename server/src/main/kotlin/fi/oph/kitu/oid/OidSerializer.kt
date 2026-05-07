package fi.oph.kitu.oid

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
