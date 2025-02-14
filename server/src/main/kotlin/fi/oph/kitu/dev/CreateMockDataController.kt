package fi.oph.kitu.dev

import fi.oph.kitu.random.generateRandomOrganizationOid
import fi.oph.kitu.random.generateRandomPerson
import fi.oph.kitu.random.getRandomLocalDate
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
import java.time.LocalDate
import java.time.ZoneOffset
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
        val suoritukset = mutableListOf<YkiSuoritusEntity>()

        for (i in 0..1000) {
            val randomPerson = generateRandomPerson()

            val tutkintopaiva =
                getRandomLocalDate(
                    LocalDate.of(2000, 1, 1),
                    LocalDate.now().minusDays(28),
                )

            val arviointipaiva =
                getRandomLocalDate(
                    tutkintopaiva,
                    LocalDate.now().minusDays(21),
                )
            val tarkistusarvioinninSaapumisPvm =
                getRandomLocalDate(
                    arviointipaiva,
                    LocalDate.now().minusDays(14),
                )
            val tarkistusarvioinninKasittelyPvm =
                getRandomLocalDate(
                    tarkistusarvioinninSaapumisPvm,
                    LocalDate.now().minusDays(7),
                )

            val lastModified =
                getRandomLocalDate(
                    tarkistusarvioinninKasittelyPvm,
                    LocalDate.now().minusDays(3),
                ).atStartOfDay()
                    .toInstant(ZoneOffset.UTC)

            suoritukset.add(
                YkiSuoritusEntity(
                    id = null,
                    suorittajanOID = randomPerson.oppijanumero.toString(),
                    hetu = randomPerson.hetu,
                    sukupuoli = randomPerson.sukupuoli,
                    sukunimi = randomPerson.sukunimi,
                    etunimet = randomPerson.etunimet,
                    kansalaisuus = randomPerson.kansalaisuus,
                    katuosoite = randomPerson.kansalaisuus,
                    postinumero = randomPerson.postinumero,
                    postitoimipaikka = randomPerson.postitoimipaikka,
                    email = randomPerson.email,
                    suoritusId = Random.nextInt(100000, 999999),
                    lastModified = lastModified,
                    tutkintopaiva = tutkintopaiva,
                    tutkintokieli = Tutkintokieli.entries.toTypedArray().random(),
                    tutkintotaso = Tutkintotaso.entries.random(),
                    jarjestajanTunnusOid = generateRandomOrganizationOid().toString(),
                    jarjestajanNimi = "${randomPerson.postitoimipaikka}n yliopisto",
                    arviointipaiva = arviointipaiva,
                    tekstinYmmartaminen = (1..5).random(),
                    kirjoittaminen = (1..5).random(),
                    rakenteetJaSanasto = (1..5).random(),
                    puheenYmmartaminen = (1..5).random(),
                    puhuminen = (1..5).random(),
                    yleisarvosana = (1..5).random(),
                    tarkistusarvioinninSaapumisPvm = tarkistusarvioinninSaapumisPvm,
                    tarkistusarvioinninAsiatunnus = (0..9999999999999).random().toString(),
                    tarkistusarvioidutOsakokeet = 2,
                    arvosanaMuuttui = 1,
                    perustelu = listOf("Erinomainen", "Hyvä", "Ihan hyvä", "Tyydyttävä", "Huono").random(),
                    tarkistusarvioinninKasittelyPvm = tarkistusarvioinninKasittelyPvm,
                ),
            )
        }

        return suoritusRepository.saveAll(suoritukset)
    }
}
