package fi.oph.kitu.webmvc

import fi.oph.kitu.koski.KoskiErrorService
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.CustomKielitestiSuoritusRepository
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.error.KielitestiSuoritusErrorRepository
import fi.oph.kitu.util.cache.InMemoryCache
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.error.YkiArvioijaErrorService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusPoikkeamaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Service
class DashboardService(
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val ykiArvioijaRepository: YkiArvioijaRepository,
    private val ykiSuoritusErrorService: YkiSuoritusErrorService,
    private val ykiArvioijaErrorService: YkiArvioijaErrorService,
    private val ykiPoikkeamaRepository: YkiSuoritusPoikkeamaRepository,
    private val customVktSuoritusRepository: CustomVktSuoritusRepository,
    private val customKielitestiSuoritusRepository: CustomKielitestiSuoritusRepository,
    private val kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
    private val koskiErrorService: KoskiErrorService,
) {
    private val ykiCache = InMemoryCache<Unit, YkiStats>(ttl = 60.seconds) { computeYki() }
    private val vktCache = InMemoryCache<Unit, VktStats>(ttl = 60.seconds) { computeVkt() }
    private val kotoCache = InMemoryCache<Unit, KotoStats>(ttl = 60.seconds) { computeKoto() }

    @WithSpan
    fun getYkiStats(): YkiStats = ykiCache.get(Unit) ?: error("ykiCache returned null; computeYki must yield non-null")

    @WithSpan
    fun getVktStats(): VktStats = vktCache.get(Unit) ?: error("vktCache returned null; computeVkt must yield non-null")

    @WithSpan
    fun getKotoStats(): KotoStats =
        kotoCache.get(Unit) ?: error("kotoCache returned null; computeKoto must yield non-null")

    @WithSpan
    fun getStats(): DashboardStats =
        DashboardStats(
            yki = getYkiStats(),
            vkt = getVktStats(),
            koto = getKotoStats(),
        )

    private fun computeYki(): YkiStats =
        YkiStats(
            suoritusCount = ykiSuoritusRepository.countSuoritukset(),
            arvioijaCount = ykiArvioijaRepository.count(),
            latestReceivedAt = ykiSuoritusRepository.findLatestLastModified(),
            suoritusImportErrorCount = ykiSuoritusErrorService.countErrors(),
            arvioijaImportErrorCount = ykiArvioijaErrorService.countErrors(),
            koskiErrorCount = koskiErrorService.countByEntity("yki", hidden = false).toLong(),
            poikkeamatCount = ykiPoikkeamaRepository.count(),
        )

    private fun computeVkt(): VktStats {
        val counts = customVktSuoritusRepository.countSuorituksetByTaitotaso()
        return VktStats(
            suoritusCount = counts.total,
            ilmoittautuneetErinomaisenTaso = counts.erinomaisenTasonIlmoittautuneet,
            suorituksetErinomaisenTaso = counts.erinomaisenTasonSuoritukset,
            suorituksetHyvaJaTyydyttavaTaso = counts.hyvanJaTyydyttavanTasonSuoritukset,
            latestReceivedAt = customVktSuoritusRepository.findLatestCreatedAt()?.toInstant(),
            koskiErrorCount = koskiErrorService.countByEntity("vkt", hidden = false).toLong(),
        )
    }

    private fun computeKoto(): KotoStats =
        KotoStats(
            suoritusCount = customKielitestiSuoritusRepository.countSuoritukset().toLong(),
            latestReceivedAt = customKielitestiSuoritusRepository.findLatestLastModified(),
            importErrorCount = kielitestiSuoritusErrorRepository.count(),
        )
}

data class DashboardStats(
    val yki: YkiStats,
    val vkt: VktStats,
    val koto: KotoStats,
)

data class YkiStats(
    val suoritusCount: Long,
    val arvioijaCount: Long,
    val latestReceivedAt: Instant?,
    val suoritusImportErrorCount: Long,
    val arvioijaImportErrorCount: Long,
    val koskiErrorCount: Long,
    val poikkeamatCount: Long,
)

data class VktStats(
    val suoritusCount: Long,
    val ilmoittautuneetErinomaisenTaso: Long,
    val suorituksetErinomaisenTaso: Long,
    val suorituksetHyvaJaTyydyttavaTaso: Long,
    val latestReceivedAt: Instant?,
    val koskiErrorCount: Long,
)

data class KotoStats(
    val suoritusCount: Long,
    val latestReceivedAt: Instant?,
    val importErrorCount: Long,
)
