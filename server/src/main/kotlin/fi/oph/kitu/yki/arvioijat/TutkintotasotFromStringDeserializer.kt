package fi.oph.kitu.yki.arvioijat

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import fi.oph.kitu.yki.Tutkintotaso

class TutkintotasotFromStringDeserializer : JsonDeserializer<Iterable<Tutkintotaso>>() {
    override fun deserialize(
        p: JsonParser?,
        ctxt: DeserializationContext?,
    ): Iterable<Tutkintotaso> = p!!.text.split("+").map { taso -> Tutkintotaso.valueOf(taso) }
}
