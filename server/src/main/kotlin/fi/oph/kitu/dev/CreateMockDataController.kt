package fi.oph.kitu.dev

import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritus
import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritusRepository
import fi.oph.kitu.mock.generateRandomKielitestiSuoritus
import fi.oph.kitu.mock.generateRandomYkiArviointiEntity
import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import kotlin.system.exitProcess

@RestController
@RequestMapping("dev")
@Profile("local", "e2e")
class CreateMockDataController(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
    private val suoritusRepository: YkiSuoritusRepository,
    private val arvioijaRepository: YkiArvioijaRepository,
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        if (environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }) {
            logger.error("Fatal error: CreateMockDataController loaded in a prod-like environment")
            exitProcess(SpringApplication.exit(applicationContext))
        }
    }

    // Yki
    @GetMapping(
        "/mockdata/yki/suoritus/",
        "/mockdata/yki/suoritus/{count}",
    )
    fun createYkiSuoritusMockData(
        @PathVariable count: Int?,
    ): Iterable<YkiSuoritusEntity> =
        suoritusRepository.saveAll(
            List(count ?: 1000) {
                generateRandomYkiSuoritusEntity()
            },
        )

    @GetMapping(
        "/mockdata/yki/arviointi/",
        "/mockdata/yki/arviointi/{count}",
    )
    fun createYkiArviointiMockData(
        @PathVariable count: Int?,
    ): Iterable<YkiArvioijaEntity> =
        arvioijaRepository.saveAll(
            List(count ?: 1000) {
                generateRandomYkiArviointiEntity()
            },
        )

    // Koto
    @GetMapping(
        "/mockdata/koto-kielitesti/suoritus/",
        "/mockdata/koto-kielitesti/suoritus/{count}",
    )
    fun createKotoSuoritusMockData(
        @PathVariable count: Int?,
    ): Iterable<KielitestiSuoritus> =
        kielitestiSuoritusRepository.saveAll(
            List(count ?: 1000) {
                generateRandomKielitestiSuoritus()
            },
        )
}
