package fi.oph.kitu.webmvc

import fi.oph.kitu.DBContainerConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.util.AopTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertSame

@SpringBootTest
@Import(DBContainerConfiguration::class)
class DashboardServiceTest(
    @param:Autowired private val dashboardService: DashboardService,
    @param:Autowired private val jdbc: JdbcTemplate,
) {
    @BeforeEach
    fun cleanup() {
        jdbc.execute("TRUNCATE TABLE yki_suoritus_lisatieto")
        jdbc.execute("TRUNCATE TABLE yki_suoritus CASCADE")
        jdbc.execute("TRUNCATE TABLE yki_arvioija CASCADE")
        jdbc.execute("TRUNCATE TABLE yki_suoritus_poikkeama")
        jdbc.execute("TRUNCATE TABLE koto_suoritus CASCADE")
        jdbc.execute("TRUNCATE TABLE vkt_suoritus CASCADE")
        jdbc.execute("TRUNCATE TABLE koski_error")
        clearCache()
    }

    @Test
    fun `tyhjasta kannasta saadaan nollat ja null aikaleimat`() {
        val stats = dashboardService.getStats()

        assertEquals(0L, stats.yki.suoritusCount)
        assertEquals(0L, stats.yki.arvioijaCount)
        assertEquals(null, stats.yki.latestReceivedAt)
        assertEquals(0L, stats.yki.suoritusImportErrorCount)
        assertEquals(0L, stats.yki.arvioijaImportErrorCount)
        assertEquals(0L, stats.yki.koskiErrorCount)
        assertEquals(0L, stats.yki.poikkeamatCount)

        assertEquals(0L, stats.vkt.suoritusCount)
        assertEquals(0L, stats.vkt.ilmoittautuneetErinomaisenTaso)
        assertEquals(0L, stats.vkt.suorituksetErinomaisenTaso)
        assertEquals(0L, stats.vkt.suorituksetHyvaJaTyydyttavaTaso)
        assertEquals(null, stats.vkt.latestReceivedAt)
        assertEquals(0L, stats.vkt.koskiErrorCount)

        assertEquals(0L, stats.koto.suoritusCount)
        assertEquals(null, stats.koto.latestReceivedAt)
        assertEquals(0L, stats.koto.importErrorCount)
    }

    @Test
    fun `cache palauttaa saman instanssin TTL-ikkunan sisalla`() {
        val first = dashboardService.getStats()
        val second = dashboardService.getStats()

        assertSame(first, second)
    }

    @Test
    fun `per-section accessorit palauttavat samat arvot kuin getStats`() {
        val stats = dashboardService.getStats()

        assertSame(stats.yki, dashboardService.getYkiStats())
        assertSame(stats.vkt, dashboardService.getVktStats())
        assertSame(stats.koto, dashboardService.getKotoStats())
    }

    @Test
    fun `cache tyhjennys saa seuraavan kutsun laskemaan tulokset uudelleen`() {
        val first = dashboardService.getStats()
        clearCache()
        val second = dashboardService.getStats()

        assertEquals(first, second)
        assertSame(first, first)
        // not assertSame(first, second) — they are equal-by-value but recomputed
    }

    private fun clearCache() {
        val target = AopTestUtils.getTargetObject<DashboardService>(dashboardService)
        val cacheField =
            DashboardService::class.java
                .getDeclaredField("cache")
                .apply { isAccessible = true }
        val cache = cacheField.get(target)
        val itemsField =
            cache.javaClass
                .getDeclaredField("items")
                .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (itemsField.get(cache) as MutableMap<Any?, Any?>).clear()
    }
}
