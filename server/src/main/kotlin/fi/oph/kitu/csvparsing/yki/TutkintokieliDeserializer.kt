package fi.oph.kitu.csvparsing.yki

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import fi.oph.kitu.yki.Tutkintokieli

class TutkintokieliDeserializer : JsonDeserializer<Tutkintokieli>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ) = when (p.text) {
        "10" -> Tutkintokieli.SWE10
        "11" -> Tutkintokieli.ENG11
        "12" -> Tutkintokieli.ENG12
        else -> Tutkintokieli.valueOf(p.text.uppercase())
    }
}
