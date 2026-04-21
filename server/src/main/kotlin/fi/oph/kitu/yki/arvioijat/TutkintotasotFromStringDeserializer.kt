package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.yki.Tutkintotaso
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class TutkintotasotFromStringDeserializer : ValueDeserializer<Any>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Iterable<Tutkintotaso> = p.string.split("+").map { taso -> Tutkintotaso.valueOf(taso) }
}
