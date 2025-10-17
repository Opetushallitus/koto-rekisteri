package fi.oph.kitu.vkt

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.OpenTelemetryTestConfig
import fi.oph.kitu.mock.VktSuoritusMockGenerator
import fi.oph.kitu.oppijanumero.OppijanumeroService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class VktSuoritusServiceTest(
    @param:Autowired private val suoritusRepository: VktSuoritusRepository,
    @param:Autowired private val customSuoritusRepository: CustomVktSuoritusRepository,
    @param:Autowired private val osakoeRepository: VktOsakoeRepository,
    @param:Autowired private val auditLogger: AuditLogger,
    @param:Autowired private val vktValidation: VktValidation,
    @param:Autowired private val oppijanumeroService: OppijanumeroService,
) {
    @BeforeEach
    fun nukeDb() {
        suoritusRepository.deleteAll()
    }

    @Test
    fun `get oppijan suoritukset`() {
        val generator = VktSuoritusMockGenerator()
        val suoritus = generator.generateRandomVktSuoritusEntity(vktValidation)
        val suoritus2 =
            generator
                .generateRandomVktSuoritusEntity(vktValidation)
                .copy(
                    suorittajanOppijanumero = suoritus.suorittajanOppijanumero,
                    etunimi = suoritus.etunimi,
                    sukunimi = suoritus.sukunimi,
                    tutkintokieli = suoritus.tutkintokieli,
                    taitotaso = suoritus.taitotaso,
                )

        suoritusRepository.saveAll(listOf(suoritus, suoritus2))

        val service =
            VktSuoritusService(
                suoritusRepository = suoritusRepository,
                customSuoritusRepository = customSuoritusRepository,
                osakoeRepository = osakoeRepository,
                auditLogger = auditLogger,
                oppijanumeroService = oppijanumeroService,
            )
        val tutkintoryhma =
            CustomVktSuoritusRepository.Tutkintoryhma(
                oppijanumero = suoritus.suorittajanOppijanumero.toString(),
                tutkintokieli = suoritus.tutkintokieli,
                taitotaso = suoritus.taitotaso,
            )

        val suoritukset = suoritusRepository.findAll()
        val osakokeet = suoritukset.flatMap { it.osakokeet }

        val henkilosuoritus = service.getOppijanSuoritukset(tutkintoryhma)
        assertNotNull(henkilosuoritus)
        assertEquals(osakokeet.size, henkilosuoritus.suoritus.osat.size)
        henkilosuoritus.suoritus.osat.forEach {
            assertEquals("Vallu Vastaanottaja", it.suorituksenVastaanottaja)
        }
    }

    @Test
    fun `get oppijan suoritukset without suorituksen vastaanottaja`() {
        val generator = VktSuoritusMockGenerator()
        val suoritus = generator.generateRandomVktSuoritusEntity(vktValidation)
        val suoritus2 =
            generator
                .generateRandomVktSuoritusEntity(vktValidation)
                .copy(
                    suorittajanOppijanumero = suoritus.suorittajanOppijanumero,
                    etunimi = suoritus.etunimi,
                    sukunimi = suoritus.sukunimi,
                    tutkintokieli = suoritus.tutkintokieli,
                    taitotaso = suoritus.taitotaso,
                )

        suoritusRepository.saveAll(listOf(suoritus, suoritus2))

        val service =
            VktSuoritusService(
                suoritusRepository = suoritusRepository,
                customSuoritusRepository = customSuoritusRepository,
                osakoeRepository = osakoeRepository,
                auditLogger = auditLogger,
                oppijanumeroService = oppijanumeroService,
            )
        val tutkintoryhma =
            CustomVktSuoritusRepository.Tutkintoryhma(
                oppijanumero = suoritus.suorittajanOppijanumero.toString(),
                tutkintokieli = suoritus.tutkintokieli,
                taitotaso = suoritus.taitotaso,
            )

        val suoritukset = suoritusRepository.findAll()
        val osakokeet = suoritukset.flatMap { it.osakokeet }

        val henkilosuoritus = service.getOppijanSuoritukset(tutkintoryhma, false)
        assertNotNull(henkilosuoritus)
        assertEquals(osakokeet.size, henkilosuoritus.suoritus.osat.size)
        henkilosuoritus.suoritus.osat.forEach {
            assertNull(it.suorituksenVastaanottaja)
        }
    }

    @Test
    fun `tutkintopaiva naytetaan oikein silloin, kun arvosana on huonompi uusintayrityksella`() {
        val suorituspohja = VktSuoritusMockGenerator().generateRandomVktSuoritusEntity(vktValidation)

        fun buildSuoritus(
            tutkintopaiva: LocalDate,
            arvosana: Koodisto.VktArvosana,
        ) = suorituspohja
            .copy(
                osakokeet =
                    suorituspohja.osakokeet
                        .map {
                            it.copy(
                                tutkintopaiva = tutkintopaiva,
                                arvosana = arvosana,
                            )
                        }.toSet(),
            ).toHenkilosuoritus()

        val ekaTutkintopaiva = suorituspohja.osakokeet.first().tutkintopaiva
        val uusinnanTutkintopaiva = ekaTutkintopaiva.plusDays(30)

        val suoritusyhdistelma =
            VktSuoritus.merge(
                listOf(
                    buildSuoritus(ekaTutkintopaiva, Koodisto.VktArvosana.Hyvä),
                    buildSuoritus(uusinnanTutkintopaiva, Koodisto.VktArvosana.Tyydyttävä),
                ),
                emptyMap(),
            )

        val tutkinto = suoritusyhdistelma.suoritus.tutkinnot.first()
        assertEquals(ekaTutkintopaiva, tutkinto.tutkintopaivaTodistuksella())
    }
}
