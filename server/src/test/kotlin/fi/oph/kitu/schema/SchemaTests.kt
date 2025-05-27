package fi.oph.kitu.schema

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.VktKirjoittamisenKoe
import fi.oph.kitu.vkt.VktPuheenYmmartamisenKoe
import fi.oph.kitu.vkt.VktPuhumisenKoe
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktTekstinYmmartamisenKoe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SchemaTests {
    companion object {
        val vktTutkintopaiva = LocalDate.of(2025, 1, 1)
        val vktHenkilosuoritus =
            Henkilosuoritus(
                henkilo =
                    OidOppija(
                        oid = OidString("1.2.246.562.10.1234567890"),
                        etunimet = "Kalle",
                        sukunimi = "Testaaja",
                    ),
                suoritus =
                    VktSuoritus(
                        osat =
                            listOf(
                                VktKirjoittamisenKoe(vktTutkintopaiva),
                                VktTekstinYmmartamisenKoe(vktTutkintopaiva),
                                VktPuhumisenKoe(vktTutkintopaiva),
                                VktPuheenYmmartamisenKoe(vktTutkintopaiva),
                            ),
                        suorituksenVastaanottaja = null,
                        suorituspaikkakunta = null,
                        taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                        kieli = Koodisto.Tutkintokieli.FIN,
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "123",
                                lahde = Lahdejarjestelma.KIOS,
                            ),
                    ),
            )
    }

    @Test
    fun `VKTSuoritus can be serialized and deserialized`() {
        val objectMapper = Henkilosuoritus.getDefaultObjectMapper()

        val json = objectMapper.writeValueAsString(vktHenkilosuoritus)

        print(json)

        val decodedData = objectMapper.readValue(json, Henkilosuoritus::class.java)

        assertEquals(expected = vktHenkilosuoritus, actual = decodedData)
    }

    @Test
    fun `Conversion to database entity and back does not change content`() {
        val entity = vktHenkilosuoritus.toVktSuoritusEntity()
        assertNotNull(entity)

        val restoredData = Henkilosuoritus.from(entity)
        assertEquals(expected = vktHenkilosuoritus, actual = restoredData)
    }
}
