package fi.oph.kitu.vkt

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.SortDirection
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.mock.VktSuoritusMockGenerator
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class VKTSuoritusRepositoryTest(
    @Autowired private var repository: VktSuoritusRepository,
    @Autowired private var customRepository: CustomVktSuoritusRepository,
    @Autowired private var postgres: PostgreSQLContainer<*>,
    @Autowired private val vktValidation: VktValidation,
) {
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

    @Test
    fun `number of rows returned for list view equals number returned by count function`() {
        val generator = VktSuoritusMockGenerator()
        repository.saveAll(List(1000) { generator.generateRandomVktSuoritusEntity(vktValidation) })

        // Erilaiset kombinaatiot, joilla funktiota testatataan
        val taitotasot =
            listOf(
                Koodisto.VktTaitotaso.Erinomainen,
                Koodisto.VktTaitotaso.HyväJaTyydyttävä,
            )
        val arvioidut =
            listOf(
                true,
                false,
                null,
            )
        val searchQuerys =
            listOf(
                null,
                "aarne",
                "1.4.2020",
            )

        taitotasot.forEach { taitotaso ->
            arvioidut.forEach { arvioidut ->
                searchQuerys.forEach { searchQuery ->
                    val suoritukset =
                        customRepository.findForListView(
                            taitotaso = taitotaso,
                            arvioidut = arvioidut,
                            column = CustomVktSuoritusRepository.Column.Sukunimi,
                            direction = SortDirection.ASC,
                            limit = 10000,
                            offset = 0,
                            searchQuery = searchQuery,
                        )

                    val count =
                        customRepository.numberOfRowsForListView(
                            taitotaso = taitotaso,
                            arvioidut = arvioidut,
                            searchQuery = searchQuery,
                        )

                    assertEquals(
                        suoritukset.size,
                        count,
                        "taitotaso=$taitotaso, arvioidut=$arvioidut, searchQuery=$searchQuery --> findForListView().size [expected] vs. numberOfRowsForListView() [actual]",
                    )
                }
            }
        }
    }
}
