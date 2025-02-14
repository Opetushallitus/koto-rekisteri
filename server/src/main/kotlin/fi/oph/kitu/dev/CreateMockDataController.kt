package fi.oph.kitu.dev

import fi.oph.kitu.random.generateRandomUserOid
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.system.exitProcess

@RestController
@RequestMapping("dev")
@Profile("local", "e2e")
class CreateMockDataController(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
    private val suoritusRepository: YkiSuoritusRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        if (environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }) {
            logger.error("Fatal error: CreateMockDataController loaded in a prod-like environment")
            exitProcess(SpringApplication.exit(applicationContext))
        }
    }

    @GetMapping("/mockdata/yki/suoritus")
    fun createYkiSuoritusMockData(): Iterable<YkiSuoritusEntity> {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val mockSuoritus =
            YkiSuoritusEntity(
                id = null,
                suorittajanOID = generateRandomUserOid(),
                hetu = "010180-9026",
                sukupuoli = Sukupuoli.N,
                sukunimi = "Öhman-Testi",
                etunimet = "Ranja Testi",
                kansalaisuus = "EST",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                email = "testi@testi.fi",
                suoritusId = 183424,
                lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                tutkintokieli = Tutkintokieli.FIN,
                tutkintotaso = Tutkintotaso.YT,
                jarjestajanTunnusOid = "1.2.246.562.10.14893989377",
                jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                arviointipaiva = LocalDate.parse("2024-11-14", dateFormatter),
                tekstinYmmartaminen = 5,
                kirjoittaminen = 4,
                rakenteetJaSanasto = 3,
                puheenYmmartaminen = 1,
                puhuminen = 2,
                yleisarvosana = 3,
                tarkistusarvioinninSaapumisPvm = LocalDate.parse("2024-10-01", dateFormatter),
                tarkistusarvioinninAsiatunnus = "123123",
                tarkistusarvioidutOsakokeet = 2,
                arvosanaMuuttui = 1,
                perustelu = "Tarkistusarvioinnin testi",
                tarkistusarvioinninKasittelyPvm = LocalDate.parse("2024-10-15", dateFormatter),
            )

        val suoritukset = mutableListOf(mockSuoritus)
        for (i in 0..1000) {
            suoritukset.add(
                mockSuoritus.copy(
                    suoritusId = Random.nextInt(100000, 999999),
                    lastModified = Instant.now(),
                ),
            )
        }
        return suoritusRepository.saveAll(suoritukset)
    }
}
