package fi.oph.kitu.dev

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import kotlin.system.exitProcess

@RestController
@RequestMapping("/dev/mock/yki")
@Profile("local", "e2e")
class MockYkiControllerApi(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        if (environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }) {
            logger.error("Fatal error: ${this::class.simpleName} loaded in a prod-like environment")
            exitProcess(SpringApplication.exit(applicationContext))
        }
    }

    @GetMapping
    fun mockYki() =
        YkiMockData(
            "Yrjö Ykittäjä",
            "010106A911C",
            "Yhdistynyt Kuningaskunta",
            "Muu",
            "Taitotalo, Helsinki",
            "suomi",
            "EVK B2/YKI 4",
            "2024-09-27 12:40",
            "Yrjönkatu 13 C, 00120 Helsinki",
        )
}

data class YkiMockData(
    val nimi: String,
    val henkilötunnus: String,
    val kansalaisuus: String,
    val sukupuoli: String,
    val tutkinnonSuorittamispaikka: String,
    val tutkintokieli: String,
    val saadutTaitotasoarviot: String,
    val tutkintokertojenAjankohta: String,
    val tarpeellisetYhteystiedot: String,
)
