package fi.oph.kitu.koodisto

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

data class KoskiKoodiviite(
    val koodiarvo: String,
    val koodistoUri: String,
) {
    companion object {
        fun from(viite: Koodisto.Koodiviite): KoskiKoodiviite =
            KoskiKoodiviite(
                koodiarvo = viite.koodiarvo,
                koodistoUri = viite.koodistoUri,
            )

        class KoskiKoodiviiteSerializer : ValueSerializer<Koodisto.Koodiviite>() {
            override fun serialize(
                value: Koodisto.Koodiviite?,
                gen: JsonGenerator?,
                serializers: SerializationContext,
            ) {
                gen?.writeStartObject()
                gen?.writeStringProperty("koodiarvo", value?.koodiarvo)
                gen?.writeStringProperty("koodistoUri", value?.koodistoUri)
                gen?.writeEndObject()
            }
        }

        class KoskiKoodiviiteModule : SimpleModule() {
            init {
                addSerializer(Koodisto.Koodiviite::class.java, KoskiKoodiviiteSerializer())
            }
        }
    }
}
