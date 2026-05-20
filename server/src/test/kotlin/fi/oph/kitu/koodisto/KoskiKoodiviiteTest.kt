package fi.oph.kitu.koodisto

import fi.oph.kitu.util.defaultObjectMapper
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class KoskiKoodiviiteTest {
    @Test
    fun `from kopioi koodiarvon ja URIn lahteesta`() {
        val koski = KoskiKoodiviite.from(Koodisto.Tutkintokieli.FIN)
        assertEquals("FI", koski.koodiarvo)
        assertEquals("kieli", koski.koodistoUri)
    }

    @Test
    fun `Koodiviite-rajapinnan toKoski tuottaa saman tuloksen kuin from`() {
        val viite = Koodisto.YkiArvosana.KT3
        assertEquals(KoskiKoodiviite.from(viite), viite.toKoski())
    }

    @Test
    fun `KoskiKoodiviiteSerializer kirjoittaa Koodiviitteen objektina jossa koodiarvo ja koodistoUri`() {
        val mapper =
            JsonMapper
                .builder()
                .addModule(KoskiKoodiviite.Companion.KoskiKoodiviiteModule())
                .build()

        val node = mapper.readTree(mapper.writeValueAsString(Koodisto.YkiTutkintotaso.YT as Koodisto.Koodiviite))

        assertEquals("yt", node["koodiarvo"].asString())
        assertEquals("ykitutkintotaso", node["koodistoUri"].asString())
    }

    @Test
    fun `KoskiKoodiviite serialisoituu defaultObjectMapperilla ilman erityismodulia`() {
        val node =
            defaultObjectMapper.readTree(
                defaultObjectMapper.writeValueAsString(KoskiKoodiviite("FI", "kieli")),
            )

        assertEquals("FI", node["koodiarvo"].asString())
        assertEquals("kieli", node["koodistoUri"].asString())
    }
}
