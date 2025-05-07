package fi.oph.kitu.vkt

import fi.oph.kitu.Oid
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class VKTSuoritusRepositoryTest(
    @Autowired private var repository: VKTSuoritusRepository,
) {
    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")!!
    }

    @BeforeEach
    fun nukeDb() {
        repository.deleteAll()
    }

    @Test
    fun `save VKT suoritus`() {
        val suoritus =
            VktSuoritusEntity(
                ilmoittautumisenId = 1,
                suorittajanOppijanumero = Oid.parseTyped("1.2.246.562.24.12345678910").getOrThrow(),
                etunimi = "Testi",
                sukunimi = "Testinen",
                tutkintokieli = Tutkintokieli.FIN,
                ilmoittautumisenTila = "COMPLETED",
                suorituskaupunki = "Helsinki",
                taitotaso = Taitotaso.Erinomainen,
                suorituksenVastaanottaja = null,
                osakokeet =
                    setOf(
                        VktOsakoe(
                            tyyppi = OsakokeenTyyppi.Puhuminen,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                        ),
                        VktOsakoe(
                            tyyppi = OsakokeenTyyppi.PuheenYmmärtäminen,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                        ),
                    ),
                tutkinnot =
                    setOf(
                        VktTutkinto(
                            tyyppi = TutkinnonTyyppi.SuullinenTaito,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                        ),
                    ),
            )
        val saved = repository.save(suoritus)
        assertEquals(
            suoritus,
            saved.copy(
                id = null,
                osakokeet = saved.osakokeet.map { it.copy(id = null) }.toSet(),
                tutkinnot = saved.tutkinnot.map { it.copy(id = null) }.toSet(),
            ),
        )
    }
}
