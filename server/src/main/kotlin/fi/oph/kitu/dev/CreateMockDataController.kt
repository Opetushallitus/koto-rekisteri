package fi.oph.kitu.dev

import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritus
import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritusRepository
import fi.oph.kitu.mock.VktSuoritusMockGenerator
import fi.oph.kitu.mock.generateRandomKielitestiSuoritus
import fi.oph.kitu.mock.generateRandomYkiArvioijaEntity
import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.mock.generateRandomYkiSuoritusErrorEntity
import fi.oph.kitu.vkt.VktSuoritusEntity
import fi.oph.kitu.vkt.VktSuoritusRepository
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorEntity
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorRepository
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
@RequestMapping("/dev")
@Profile("local", "e2e")
class CreateMockDataController(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
    private val suoritusRepository: YkiSuoritusRepository,
    private val suoritusErrorRepository: YkiSuoritusErrorRepository,
    private val arvioijaRepository: YkiArvioijaRepository,
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    private val vktSuoritusRepository: VktSuoritusRepository,
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

    // Yki
    @GetMapping(
        "/mockdata/yki/suoritus/virheet",
        "/mockdata/yki/suoritus/virheet/{count}",
    )
    fun createYkiSuoritusErrorsMockData(
        @PathVariable count: Int?,
    ): Iterable<YkiSuoritusErrorEntity> =
        suoritusErrorRepository
            .saveAll(
                List(count ?: 3) {
                    generateRandomYkiSuoritusErrorEntity()
                },
            )

    @GetMapping(
        "/mockdata/yki/arvioija/",
        "/mockdata/yki/arvioija/{count}",
    )
    fun createYkiArvioijaMockData(
        @PathVariable count: Int?,
    ): Iterable<YkiArvioijaEntity> =
        arvioijaRepository.saveAll(
            List(count ?: 1000) {
                generateRandomYkiArvioijaEntity()
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

    // Vkt
    @GetMapping(
        "/mockdata/vkt/suoritus/",
        "/mockdata/vkt/suoritus/{count}",
    )
    fun createVktSuoritusMockData(
        @PathVariable count: Int?,
    ): Iterable<VktSuoritusEntity> {
        val generator = VktSuoritusMockGenerator()
        return vktSuoritusRepository.saveAll(
            List(count ?: 1000) {
                generator.generateRandomVktSuoritusEntity()
            },
        )
    }
}
