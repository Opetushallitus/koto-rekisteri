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
import kotlin.test.assertNotNull


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
        val suoritus = VKTSuoritusEntity(
            ilmoittautumisenId = 1,
            suorittajanOppijanumero = Oid.parseTyped("1.2.246.562.24.12345678910").getOrThrow(),
            etunimi = "Testi",
            sukunimi = "Testinen",
            tutkintokieli = Tutkintokieli.FIN,
            tutkintopaiva = LocalDate.of(2025, 1, 1),
            ilmoittautumisenTila = "COMPLETED",
            ilmoittautunutPuhuminen = true,
            ilmoittautunutPuheenYmmartaminen = true,
            ilmoittautunutKirjoittaminen = true,
            ilmoittautunutTekstinYmmartaminen = true,
            suorituskaupunki = "Helsinki",
            taitotaso = Taitotaso.Erinomainen,
            suorituksenVastaanottaja = null,
            puhuminen = null,
            puheenYmmartaminen = null,
            kirjoittaminen = null,
            tekstinYmmartaminen = null,
            suullinenTaito = null,
            kirjallinenTaito = null,
            ymmartamisenTaito = null
        )
        val saved = repository.save(suoritus)
        assertNotNull(saved.id)
        assertEquals(suoritus, saved.copy(id = null))
    }
}
