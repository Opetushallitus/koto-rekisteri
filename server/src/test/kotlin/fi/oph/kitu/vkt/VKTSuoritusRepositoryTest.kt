package fi.oph.kitu.vkt

import fi.oph.kitu.Oid
import fi.oph.kitu.koodisto.Koodisto
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class VKTSuoritusRepositoryTest(
    @Autowired private var repository: VktSuoritusRepository,
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
                ilmoittautumisenId = "1",
                suorittajanOppijanumero = Oid.parseTyped("1.2.246.562.24.12345678910").getOrThrow(),
                etunimi = "Testi",
                sukunimi = "Testinen",
                tutkintokieli = Koodisto.Tutkintokieli.FIN,
                ilmoittautumisenTila = "COMPLETED",
                suorituspaikkakunta = "Helsinki",
                taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                suorituksenVastaanottaja = null,
                osakokeet =
                    setOf(
                        VktSuoritusEntity.VktOsakoe(
                            tyyppi = Koodisto.VktOsakoe.Puhuminen,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                        ),
                        VktSuoritusEntity.VktOsakoe(
                            tyyppi = Koodisto.VktOsakoe.PuheenYmmärtäminen,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                        ),
                    ),
                tutkinnot =
                    setOf(
                        VktSuoritusEntity.VktTutkinto(
                            tyyppi = Koodisto.VktKielitaito.Suullinen,
                            arviointipaiva = null,
                            arvosana = null,
                        ),
                    ),
            )
        val suoritusId = repository.save(suoritus).id!!
        val savedSuoritus = repository.findById(suoritusId).getOrNull()!!
        assertEquals(
            suoritus,
            savedSuoritus.copy(
                id = null,
                osakokeet = savedSuoritus.osakokeet.map { it.copy(id = null) }.toSet(),
                tutkinnot = savedSuoritus.tutkinnot.map { it.copy(id = null) }.toSet(),
                createdAt = null,
            ),
        )
    }
}
