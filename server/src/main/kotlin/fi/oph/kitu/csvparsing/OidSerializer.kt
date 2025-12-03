package fi.oph.kitu.csvparsing

import fi.oph.kitu.Oid
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer

class OidSerializer : StdSerializer<Oid>(Oid::class.java) {
    override fun serialize(
        value: Oid?,
        gen: JsonGenerator?,
        provider: SerializationContext?,
    ) {
        gen?.writeString(value?.toString())
    }
}
