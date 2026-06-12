package fi.oph.kitu.yki

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period
import kotlin.test.assertEquals

class AnomalyCheckLookbackTest {
    @Test
    fun `lauantaina tarkistetaan  koko vuoden ajalta`() {
        assertEquals(Period.ofYears(1), anomalyCheckLookback(LocalDate.of(2026, 6, 6)))
    }

    @Test
    fun `arkipäivänä tarkistetaan yhden kuukauden ajalta`() {
        assertEquals(Period.ofMonths(1), anomalyCheckLookback(LocalDate.of(2026, 6, 8)))
        assertEquals(Period.ofMonths(1), anomalyCheckLookback(LocalDate.of(2026, 6, 14)))
    }
}
