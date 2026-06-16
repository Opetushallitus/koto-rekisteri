package fi.oph.kitu.webmvc

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.dev.mockdata.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.util.AopTestUtils
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertSame

@SpringBootTest
@Import(DBContainerConfiguration::class)
class DashboardServiceTest(
    @param:Autowired private val dashboardService: DashboardService,
    @param:Autowired private val jdbc: JdbcTemplate,
    @param:Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
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
        jdbc.execute("TRUNCATE TABLE tehtavapaketti CASCADE")
        clearAllCaches()
    }

    @Test
    fun `tyhjasta kannasta saadaan nollat ja null aikaleimat`() {
        val stats = dashboardService.getStats()

        assertEquals(0L, stats.yki.suoritusCount)
        assertEquals(0L, stats.yki.arvioijaCount)
        assertEquals(0L, stats.yki.tarkistusarvioinnitOdottamassaCount)
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
        assertEquals(0L, stats.koto.tehtavapaketitCount)
        assertEquals(null, stats.koto.latestReceivedAt)
        assertEquals(0L, stats.koto.importErrorCount)
    }

    @Test
    fun `per-section accessor palauttaa saman instanssin TTL-ikkunan sisalla`() {
        assertSame(dashboardService.getYkiStats(), dashboardService.getYkiStats())
        assertSame(dashboardService.getVktStats(), dashboardService.getVktStats())
        assertSame(dashboardService.getKotoStats(), dashboardService.getKotoStats())
        assertSame(dashboardService.getAdminStats(), dashboardService.getAdminStats())
    }

    @Test
    fun `vain hidas sektio kuluttaa hidasta laskentaa ja muut sektiot palaavat erikseen`() {
        val ykiBefore = dashboardService.getYkiStats()
        clearVktCache()

        assertSame(ykiBefore, dashboardService.getYkiStats(), "YKI-cache ei tyhjentynyt VKT-tyhjennyksen mukana")
    }

    @Test
    fun `tarkistusarvioinnin hyvaksynta ei muuta YKI viimeisin saapunut -aikaleimaa`() {
        val solkiId = 654321
        val ulkoinenSaapumisaika = Instant.parse("2026-05-01T08:00:00Z")
        val virkailijanHyvaksyntaaika = Instant.parse("2026-06-01T12:00:00Z")

        val ulkoinenSuoritus =
            generateRandomYkiSuoritusEntity().copy(
                solkiId = solkiId,
                lastModified = ulkoinenSaapumisaika,
                receivedAt = ulkoinenSaapumisaika,
                arviointitila = Arviointitila.ARVIOITU,
            )
        ykiSuoritusRepository.save(ulkoinenSuoritus, false)
        clearAllCaches()
        assertEquals(ulkoinenSaapumisaika, dashboardService.getYkiStats().latestReceivedAt)

        // Virkailija hyväksyy tarkistusarvioinnin: data class .copy() säilyttää receivedAt:n alkuperäisessä arvossa.
        val hyvaksyntaVersio =
            ulkoinenSuoritus.copy(
                id = null,
                lastModified = virkailijanHyvaksyntaaika,
                arviointitila = Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
            )
        ykiSuoritusRepository.save(hyvaksyntaVersio, true)
        clearAllCaches()

        assertEquals(
            ulkoinenSaapumisaika,
            dashboardService.getYkiStats().latestReceivedAt,
            "received_at ei saa edetä virkailijan tarkistusarvioinnin hyväksymisestä",
        )
    }

    @Test
    fun `cache tyhjennys saa seuraavan kutsun laskemaan tulokset uudelleen`() {
        val first = dashboardService.getYkiStats()
        clearAllCaches()
        val second = dashboardService.getYkiStats()

        assertEquals(first, second)
    }

    private fun clearAllCaches() {
        clearCache("ykiCache")
        clearCache("vktCache")
        clearCache("kotoCache")
        clearCache("adminCache")
    }

    private fun clearVktCache() = clearCache("vktCache")

    private fun clearCache(fieldName: String) {
        val target = AopTestUtils.getTargetObject<DashboardService>(dashboardService)
        val cacheField =
            DashboardService::class.java
                .getDeclaredField(fieldName)
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
