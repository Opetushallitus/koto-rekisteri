package fi.oph.kitu.i18n

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class RelativeTimeTest {
    private val now: Instant = Instant.parse("2026-05-29T12:00:00Z")

    @Test
    fun `null aika palauttaa viivan`() {
        assertEquals("—", formatRelativeTime(null, now))
    }

    @Test
    fun `alle minuutti sitten on juuri nyt`() {
        assertEquals("juuri nyt", formatRelativeTime(now.minusSeconds(0), now))
        assertEquals("juuri nyt", formatRelativeTime(now.minusSeconds(59), now))
    }

    @Test
    fun `alle tunti sitten naytetaan minuutteina`() {
        assertEquals("1 min sitten", formatRelativeTime(now.minusSeconds(60), now))
        assertEquals("59 min sitten", formatRelativeTime(now.minusSeconds(59 * 60), now))
    }

    @Test
    fun `alle vuorokausi sitten naytetaan tunteina`() {
        assertEquals("1 t sitten", formatRelativeTime(now.minusSeconds(60 * 60), now))
        assertEquals("23 t sitten", formatRelativeTime(now.minusSeconds(23 * 60 * 60), now))
    }

    @Test
    fun `eilen ja paivia sitten`() {
        assertEquals("eilen", formatRelativeTime(now.minusSeconds(24 * 60 * 60), now))
        assertEquals("eilen", formatRelativeTime(now.minusSeconds(2 * 24 * 60 * 60 - 1), now))
        assertEquals("2 pv sitten", formatRelativeTime(now.minusSeconds(2 * 24 * 60 * 60), now))
        assertEquals("6 pv sitten", formatRelativeTime(now.minusSeconds(6 * 24 * 60 * 60), now))
    }

    @Test
    fun `viikkoa vanhempi nayttaa paivamaaran`() {
        val sevenDaysAgo = now.minusSeconds(7L * 24 * 60 * 60)
        val formatted = formatRelativeTime(sevenDaysAgo, now)
        assertEquals(true, formatted.matches(Regex("""\d{1,2}\.\d{1,2}\.\d{4}""")), "Got: $formatted")
    }

    @Test
    fun `suhteellinen aika kaannetaan annetulle kielelle`() {
        assertEquals("just nu", formatRelativeTime(now.minusSeconds(0), now, Language.SV))
        assertEquals("1 min sedan", formatRelativeTime(now.minusSeconds(60), now, Language.SV))
        assertEquals("1 h sedan", formatRelativeTime(now.minusSeconds(60 * 60), now, Language.SV))
        assertEquals("igår", formatRelativeTime(now.minusSeconds(24 * 60 * 60), now, Language.SV))
        assertEquals("2 dgr sedan", formatRelativeTime(now.minusSeconds(2 * 24 * 60 * 60), now, Language.SV))

        assertEquals("just now", formatRelativeTime(now.minusSeconds(0), now, Language.EN))
        assertEquals("1 min ago", formatRelativeTime(now.minusSeconds(60), now, Language.EN))
        assertEquals("1 h ago", formatRelativeTime(now.minusSeconds(60 * 60), now, Language.EN))
        assertEquals("yesterday", formatRelativeTime(now.minusSeconds(24 * 60 * 60), now, Language.EN))
        assertEquals("2 days ago", formatRelativeTime(now.minusSeconds(2 * 24 * 60 * 60), now, Language.EN))
    }
}
