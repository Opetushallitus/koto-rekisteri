package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.yki.Arviointitila
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("DEPRECATION")
class YkiArviointitilaLaskentaTest {
    private fun rivi(
        nykyinen: Arviointitila,
        osakoeCount: Int = 0,
        nullCount: Int = 0,
        realGradeCount: Int = 0,
        onTarkistusarviointi: Boolean = false,
        tarkistuksenKasittelypaiva: LocalDate? = null,
    ) = ArviointitilanMigraatiorivi(
        id = 1,
        nykyinenTila = nykyinen,
        osakoeCount = osakoeCount,
        nullCount = nullCount,
        realGradeCount = realGradeCount,
        onTarkistusarviointi = onTarkistusarviointi,
        tarkistuksenKasittelypaiva = tarkistuksenKasittelypaiva,
    )

    @Test
    fun `KESKEYTETTY ilman oikeita arvosanoja muuttuu EI_SUORITUSTA-tilaan`() {
        assertEquals(
            Arviointitila.EI_SUORITUSTA,
            laskeUusiArviointitila(rivi(Arviointitila.KESKEYTETTY, osakoeCount = 3, realGradeCount = 0)),
        )
    }

    @Test
    fun `KESKEYTETTY jossa on oikea arvosana muuttuu ARVIOITU-tilaan`() {
        assertEquals(
            Arviointitila.ARVIOITU,
            laskeUusiArviointitila(rivi(Arviointitila.KESKEYTETTY, osakoeCount = 3, realGradeCount = 1)),
        )
    }

    @Test
    fun `ARVIOITU ilman oikeita arvosanoja muuttuu EI_SUORITUSTA-tilaan`() {
        assertEquals(
            Arviointitila.EI_SUORITUSTA,
            laskeUusiArviointitila(rivi(Arviointitila.ARVIOITU, osakoeCount = 4, realGradeCount = 0)),
        )
    }

    @Test
    fun `ARVIOITU jossa on oikea arvosana sailyy`() {
        assertEquals(
            Arviointitila.ARVIOITU,
            laskeUusiArviointitila(rivi(Arviointitila.ARVIOITU, osakoeCount = 4, realGradeCount = 2)),
        )
    }

    @Test
    fun `osakoe ilman arvosanaa tuottaa ARVIOITAVA-tilan`() {
        assertEquals(
            Arviointitila.ARVIOITAVA,
            laskeUusiArviointitila(rivi(Arviointitila.ARVIOITU, osakoeCount = 4, nullCount = 1, realGradeCount = 2)),
        )
    }

    @Test
    fun `ilman osakokeita tila sailyy ennallaan`() {
        assertEquals(
            Arviointitila.ARVIOITAVA,
            laskeUusiArviointitila(rivi(Arviointitila.ARVIOITAVA, osakoeCount = 0)),
        )
    }

    @Test
    fun `tarkistusarviointi ilman kasittelypaivaa tuottaa TARKISTUSARVIOITAVA-tilan`() {
        assertEquals(
            Arviointitila.TARKISTUSARVIOITAVA,
            laskeUusiArviointitila(
                rivi(Arviointitila.ARVIOITU, osakoeCount = 3, realGradeCount = 1, onTarkistusarviointi = true),
            ),
        )
    }

    @Test
    fun `tarkistusarviointi kasittelypaivalla tuottaa TARKISTUSARVIOITU-tilan`() {
        assertEquals(
            Arviointitila.TARKISTUSARVIOITU,
            laskeUusiArviointitila(
                rivi(
                    Arviointitila.ARVIOITU,
                    osakoeCount = 3,
                    realGradeCount = 1,
                    onTarkistusarviointi = true,
                    tarkistuksenKasittelypaiva = LocalDate.of(2025, 1, 1),
                ),
            ),
        )
    }

    @Test
    fun `ILMOITTAUTUNUT sailyy vaikka osakokeilla ei ole arvosanaa`() {
        assertEquals(
            Arviointitila.ILMOITTAUTUNUT,
            laskeUusiArviointitila(rivi(Arviointitila.ILMOITTAUTUNUT, osakoeCount = 3, nullCount = 3)),
        )
    }

    @Test
    fun `PERUTTU sailyy`() {
        assertEquals(
            Arviointitila.PERUTTU,
            laskeUusiArviointitila(rivi(Arviointitila.PERUTTU, osakoeCount = 0)),
        )
    }

    @Test
    fun `TARKISTUSARVIOINTI_HYVAKSYTTY sailyy vaikka tarkistustietoja olisi`() {
        assertEquals(
            Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
            laskeUusiArviointitila(
                rivi(
                    Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
                    osakoeCount = 3,
                    realGradeCount = 1,
                    onTarkistusarviointi = true,
                    tarkistuksenKasittelypaiva = LocalDate.of(2025, 1, 1),
                ),
            ),
        )
    }
}
