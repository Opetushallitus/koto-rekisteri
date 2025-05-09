package fi.oph.kitu.koodisto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule

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

        class KoskiKoodiviiteSerializer : JsonSerializer<Koodisto.Koodiviite>() {
            override fun serialize(
                value: Koodisto.Koodiviite?,
                gen: JsonGenerator?,
                serializers: SerializerProvider?,
            ) {
                gen?.writeStartObject()
                gen?.writeStringField("koodiarvo", value?.koodiarvo)
                gen?.writeStringField("koodistoUri", value?.koodistoUri)
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
