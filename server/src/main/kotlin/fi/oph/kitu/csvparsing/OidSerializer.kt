package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.ietf.jgss.Oid

class OidSerializer : JsonSerializer<Oid>() {
    override fun serialize(
        oid: Oid?,
        jsonGenerator: JsonGenerator?,
        serializerProvider: SerializerProvider?,
    ) {
        jsonGenerator?.writeString(oid?.toString())
    }
}
