package fi.oph.kitu.yki

import fi.oph.kitu.yki.arvioijat.Arviointikausi
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class ArviointikausiTest {
    @Test
    fun `kausi paattyy viiden vuoden paasta samana paivana`() {
        assertEquals(
            LocalDate.of(2020, 12, 7),
            Arviointikausi.paattymispaiva(LocalDate.of(2015, 12, 7)),
        )
    }

    @Test
    fun `karkauspaivalta alkava kausi paattyy helmikuun viimeisena`() {
        assertEquals(
            LocalDate.of(2029, 2, 28),
            Arviointikausi.paattymispaiva(LocalDate.of(2024, 2, 29)),
        )
    }

    @Test
    fun `kauden pituus on viisi vuotta`() {
        assertEquals(5L, Arviointikausi.KAUDEN_PITUUS_VUOSINA)
    }
}
