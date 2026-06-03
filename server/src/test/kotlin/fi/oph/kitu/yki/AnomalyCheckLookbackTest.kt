package fi.oph.kitu.yki

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period
import kotlin.test.assertEquals

class AnomalyCheckLookbackTest {
    @Test
    fun `kuun 1 päivänä tarkistetaan koko vuoden ajalta`() {
        assertEquals(Period.ofYears(1), anomalyCheckLookback(LocalDate.of(2026, 1, 1)))
        assertEquals(Period.ofYears(1), anomalyCheckLookback(LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun `sunnuntaina tarkistetaan kolmen kuukauden ajalta`() {
        assertEquals(Period.ofMonths(3), anomalyCheckLookback(LocalDate.of(2026, 6, 7)))
    }

    @Test
    fun `arkipäivänä tarkistetaan yhden kuukauden ajalta`() {
        assertEquals(Period.ofMonths(1), anomalyCheckLookback(LocalDate.of(2026, 6, 8)))
        assertEquals(Period.ofMonths(1), anomalyCheckLookback(LocalDate.of(2026, 6, 13)))
    }

    @Test
    fun `kuun 1 päivän sääntö voittaa kun se osuu sunnuntaille`() {
        assertEquals(Period.ofYears(1), anomalyCheckLookback(LocalDate.of(2026, 3, 1)))
    }
}
