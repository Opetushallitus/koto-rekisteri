package fi.oph.kitu.vkt

import fi.oph.kitu.koodisto.Koodisto
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * Puutelistojen jarjestys nakyy kayttoliittymassa, joten se ei saa riippua siita missa
 * jarjestyksessa osakokeet sattuvat tulemaan kannasta.
 */
class VktPuuttuvatArvioinnitTest {
    @Test
    fun `puuttuvat arvioinnit ovat samassa jarjestyksessa riippumatta osakokeiden jarjestyksesta`() {
        val puhuminenEnsin =
            puuttuvatArvioinnit(
                Koodisto.VktKielitaito.Suullinen,
                Koodisto.VktOsakoe.Puhuminen,
                Koodisto.VktOsakoe.PuheenYmmärtäminen,
            )
        val ymmartaminenEnsin =
            puuttuvatArvioinnit(
                Koodisto.VktKielitaito.Suullinen,
                Koodisto.VktOsakoe.PuheenYmmärtäminen,
                Koodisto.VktOsakoe.Puhuminen,
            )

        assertEquals(puhuminenEnsin, ymmartaminenEnsin)
        assertEquals(
            listOf(Koodisto.VktOsakoe.PuheenYmmärtäminen, Koodisto.VktOsakoe.Puhuminen),
            puhuminenEnsin,
        )
    }

    @Test
    fun `puuttuvat arvioinnit noudattavat samaa jarjestysta kuin puuttuvat osakokeet`() {
        val tutkinto =
            tutkinto(
                Koodisto.VktKielitaito.Ymmärtäminen,
                Koodisto.VktOsakoe.TekstinYmmärtäminen,
                Koodisto.VktOsakoe.PuheenYmmärtäminen,
            )

        assertEquals(tutkinto.mahdollisetOsakokeidenTyypit(), tutkinto.puuttuvatArvioinnit())
    }

    private fun puuttuvatArvioinnit(
        kielitaito: Koodisto.VktKielitaito,
        vararg osakokeet: Koodisto.VktOsakoe,
    ): List<Koodisto.VktOsakoe> = tutkinto(kielitaito, *osakokeet).puuttuvatArvioinnit()

    private fun tutkinto(
        kielitaito: Koodisto.VktKielitaito,
        vararg osakokeet: Koodisto.VktOsakoe,
    ): VktTutkinto =
        VktTutkinto
            .from(
                VktSuoritusEntity.VktTutkinto(tyyppi = kielitaito, arviointipaiva = null, arvosana = null),
                osakokeet
                    .map { tyyppi ->
                        VktSuoritusEntity.VktOsakoe(
                            tyyppi = tyyppi,
                            tutkintopaiva = LocalDate.of(2024, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                            merkittyPoistettavaksi = null,
                        )
                    }.toCollection(LinkedHashSet()),
            ).single()
}
