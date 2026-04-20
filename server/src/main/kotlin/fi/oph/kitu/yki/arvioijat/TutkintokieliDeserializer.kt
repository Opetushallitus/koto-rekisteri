package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.yki.Tutkintokieli
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class TutkintokieliDeserializer : ValueDeserializer<Any>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Tutkintokieli =
        when (p.text) {
            "10" -> Tutkintokieli.SWE10
            "11" -> Tutkintokieli.ENG11
            "12" -> Tutkintokieli.ENG12
            else -> Tutkintokieli.valueOf(p.text.uppercase())
        }
}
