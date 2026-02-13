package fi.oph.kitu.vkt

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.SortDirection
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.mock.VktSuoritusMockGenerator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNull
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
    @param:Autowired private var repository: VktSuoritusRepository,
    @param:Autowired private var customRepository: CustomVktSuoritusRepository,
    @param:Autowired private var postgres: PostgreSQLContainer<*>,
    @param:Autowired private val vktValidation: VktValidation,
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
                suorittajanOid = Oid.parseTyped("1.2.246.562.24.12345678910").getOrThrow(),
                etunimet = "Testi",
                sukunimi = "Testinen",
                tutkintokieli = Koodisto.Tutkintokieli.FIN,
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
                            merkittyPoistettavaksi = null,
                        ),
                        VktSuoritusEntity.VktOsakoe(
                            tyyppi = Koodisto.VktOsakoe.PuheenYmmärtäminen,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                            merkittyPoistettavaksi = null,
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

    @Test
    fun `find suoritukset for csv`() {
        val suoritukset =
            listOf(
                VktSuoritusEntity(
                    ilmoittautumisenId = "1",
                    suorittajanOid = Oid.parseTyped("1.2.246.562.24.12345678910").getOrThrow(),
                    etunimet = "Testi",
                    sukunimi = "Testinen",
                    tutkintokieli = Koodisto.Tutkintokieli.FIN,
                    suorituspaikkakunta = "Helsinki",
                    taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                    suorituksenVastaanottaja = Oid.parse("1.2.246.562.24.91757873900").getOrNull(),
                    osakokeet =
                        setOf(
                            VktSuoritusEntity.VktOsakoe(
                                tyyppi = Koodisto.VktOsakoe.Puhuminen,
                                tutkintopaiva = LocalDate.of(2025, 1, 1),
                                arviointipaiva = LocalDate.of(2025, 1, 2),
                                arvosana = Koodisto.VktArvosana.Erinomainen,
                                merkittyPoistettavaksi = null,
                            ),
                            VktSuoritusEntity.VktOsakoe(
                                tyyppi = Koodisto.VktOsakoe.PuheenYmmärtäminen,
                                tutkintopaiva = LocalDate.of(2025, 1, 1),
                                arviointipaiva = null,
                                arvosana = null,
                                merkittyPoistettavaksi = null,
                            ),
                        ),
                    tutkinnot = setOf(),
                ),
                VktSuoritusEntity(
                    ilmoittautumisenId = "2",
                    suorittajanOid = Oid.parseTyped("1.2.246.562.24.12345678912").getOrThrow(),
                    etunimet = "Maija",
                    sukunimi = "Mehiläinen",
                    tutkintokieli = Koodisto.Tutkintokieli.FIN,
                    suorituspaikkakunta = "Helsinki",
                    taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                    suorituksenVastaanottaja = Oid.parse("1.2.246.562.24.91757873900").getOrNull(),
                    osakokeet =
                        setOf(
                            VktSuoritusEntity.VktOsakoe(
                                tyyppi = Koodisto.VktOsakoe.Puhuminen,
                                tutkintopaiva = LocalDate.of(2025, 1, 1),
                                arviointipaiva = LocalDate.of(2025, 1, 2),
                                arvosana = Koodisto.VktArvosana.Erinomainen,
                                merkittyPoistettavaksi = null,
                            ),
                            VktSuoritusEntity.VktOsakoe(
                                tyyppi = Koodisto.VktOsakoe.PuheenYmmärtäminen,
                                tutkintopaiva = LocalDate.of(2025, 1, 1),
                                arviointipaiva = LocalDate.of(2025, 1, 2),
                                arvosana = Koodisto.VktArvosana.Erinomainen,
                                merkittyPoistettavaksi = null,
                            ),
                            VktSuoritusEntity.VktOsakoe(
                                tyyppi = Koodisto.VktOsakoe.Kirjoittaminen,
                                tutkintopaiva = LocalDate.of(2025, 1, 1),
                                arviointipaiva = LocalDate.of(2025, 1, 2),
                                arvosana = Koodisto.VktArvosana.Hylätty,
                                merkittyPoistettavaksi = null,
                            ),
                            VktSuoritusEntity.VktOsakoe(
                                tyyppi = Koodisto.VktOsakoe.TekstinYmmärtäminen,
                                tutkintopaiva = LocalDate.of(2025, 1, 1),
                                arviointipaiva = LocalDate.of(2025, 1, 2),
                                arvosana = Koodisto.VktArvosana.Erinomainen,
                                merkittyPoistettavaksi = null,
                            ),
                        ),
                    tutkinnot = setOf(),
                ),
            )

        repository.saveAll(suoritukset)
        val suorituksetCsv = customRepository.findAllForCsv()
        val suoritus1Csv = suorituksetCsv.first { it.ilmoittautumisenId == "1" }
        val suoritus2Csv = suorituksetCsv.first { it.ilmoittautumisenId == "2" }
        assertAll(
            fun() = assertEquals(2, suorituksetCsv.count()),
            fun() = assertEquals("1.2.246.562.24.12345678910", suoritus1Csv.suorittajanOid),
            fun() = assertEquals("Erinomainen", suoritus1Csv.puhuminen),
            fun() = assertNull(suoritus1Csv.puheenYmmartaminen),
            fun() = assertNull(suoritus1Csv.kirjoittaminen),
            fun() = assertNull(suoritus1Csv.tekstinYmmartaminen),
            fun() = assertEquals("1.2.246.562.24.12345678912", suoritus2Csv.suorittajanOid),
            fun() = assertEquals("Erinomainen", suoritus2Csv.puhuminen),
            fun() = assertEquals("Erinomainen", suoritus2Csv.puheenYmmartaminen),
            fun() = assertEquals("Hylätty", suoritus2Csv.kirjoittaminen),
            fun() = assertEquals("Erinomainen", suoritus2Csv.tekstinYmmartaminen),
        )
    }

    @Test
    fun `find suoritukset for csv doesn't return suoritus with no arvosana`() {
        val suoritus =
            VktSuoritusEntity(
                ilmoittautumisenId = "1",
                suorittajanOid = Oid.parseTyped("1.2.246.562.24.12345678910").getOrThrow(),
                etunimet = "Testi",
                sukunimi = "Testinen",
                tutkintokieli = Koodisto.Tutkintokieli.FIN,
                suorituspaikkakunta = "Helsinki",
                taitotaso = Koodisto.VktTaitotaso.Erinomainen,
                suorituksenVastaanottaja = Oid.parse("1.2.246.562.24.91757873900").getOrNull(),
                osakokeet =
                    setOf(
                        VktSuoritusEntity.VktOsakoe(
                            tyyppi = Koodisto.VktOsakoe.Puhuminen,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                            merkittyPoistettavaksi = null,
                        ),
                        VktSuoritusEntity.VktOsakoe(
                            tyyppi = Koodisto.VktOsakoe.PuheenYmmärtäminen,
                            tutkintopaiva = LocalDate.of(2025, 1, 1),
                            arviointipaiva = null,
                            arvosana = null,
                            merkittyPoistettavaksi = null,
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
        repository.save(suoritus)
        val suoritukset = customRepository.findAllForCsv()
        assertEquals(0, suoritukset.count())
    }
}
