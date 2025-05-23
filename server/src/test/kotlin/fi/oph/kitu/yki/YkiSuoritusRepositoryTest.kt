package fi.oph.kitu.yki

import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Testcontainers
class YkiSuoritusRepositoryTest(
    @Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
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
        ykiSuoritusRepository.deleteAll()
    }

    @Test
    fun `suoritus is saved correctly`() {
        val suoritus = generateRandomYkiSuoritusEntity()
        val savedSuoritukset = ykiSuoritusRepository.saveAll(listOf(suoritus)).toList()
        assertEquals(suoritus, savedSuoritukset[0].copy(id = null))
    }

    @Test
    fun `saveAll returns only the saved suoritus`() {
        val initialSuoritus = generateRandomYkiSuoritusEntity()
        ykiSuoritusRepository.saveAll(listOf(initialSuoritus)).toList()
        val updatedSuoritus =
            initialSuoritus.copy(
                sukupuoli = Sukupuoli.E,
                lastModified = Instant.parse("2025-01-01T13:53:56Z"),
            )
        val savedSuoritukset = ykiSuoritusRepository.saveAll(listOf(initialSuoritus, updatedSuoritus))
        assertEquals(1, savedSuoritukset.count())
        assertEquals(updatedSuoritus, savedSuoritukset.elementAt(0).copy(id = null))
    }

    @Test
    fun `suoritus with null values is saved correctly`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                email = null,
                tekstinYmmartaminen = null,
                kirjoittaminen = null,
                rakenteetJaSanasto = null,
                puheenYmmartaminen = null,
                puhuminen = null,
                yleisarvosana = null,
                tarkistusarvioinninSaapumisPvm = null,
                tarkistusarvioinninAsiatunnus = null,
                tarkistusarvioidutOsakokeet = null,
                arvosanaMuuttui = null,
                perustelu = null,
                tarkistusarvioinninKasittelyPvm = null,
                koskiOpiskeluoikeus = null,
            )
        val savedSuoritukset = ykiSuoritusRepository.saveAll(listOf(suoritus)).toList()
        assertEquals(suoritus, savedSuoritukset[0].copy(id = null))
    }

    @Test
    fun `finding distinct suoritukset returns the latest suoritus of same suoritusId`() {
        val suoritus = generateRandomYkiSuoritusEntity(maxDate = LocalDate.of(2024, 9, 1))
        val suoritus2 = generateRandomYkiSuoritusEntity(maxDate = LocalDate.of(2024, 9, 1))

        val updatedSuoritus =
            suoritus.copy(
                lastModified = Instant.parse("2024-11-01T13:53:56Z"),
                tekstinYmmartaminen = 5,
                kirjoittaminen = 4,
                rakenteetJaSanasto = 3,
                puheenYmmartaminen = 1,
                puhuminen = 2,
                yleisarvosana = 3,
                tarkistusarvioinninSaapumisPvm = LocalDate.of(2024, 10, 1),
                tarkistusarvioinninAsiatunnus = "123123",
                tarkistusarvioidutOsakokeet = 2,
                arvosanaMuuttui = 4,
                perustelu = "Tarkistusarvioinnin testi",
                tarkistusarvioinninKasittelyPvm = LocalDate.of(2024, 10, 15),
            )
        ykiSuoritusRepository.saveAll(listOf(suoritus, suoritus2, updatedSuoritus))

        val suoritukset =
            ykiSuoritusRepository
                .find(distinct = true)
                .map { it.copy(id = null) }
                .toList()

        assertAll(
            fun() = assertContains(suoritukset, updatedSuoritus),
            fun() = assertContains(suoritukset, suoritus2),
            fun() = assertFalse(suoritukset.contains(suoritus)),
            fun() = assertEquals(2, suoritukset.count()),
        )
    }

    @Test
    fun `suoritukset with legacy language codes are saved correctly`() {
        val suoritusSWE10 = generateRandomYkiSuoritusEntity().copy(tutkintokieli = Tutkintokieli.SWE10)
        val suoritusENG11 = generateRandomYkiSuoritusEntity().copy(tutkintokieli = Tutkintokieli.ENG11)
        val suoritusENG12 = generateRandomYkiSuoritusEntity().copy(tutkintokieli = Tutkintokieli.ENG12)
        val suoritukset = listOf(suoritusSWE10, suoritusENG11, suoritusENG12)
        val savedSuoritukset = ykiSuoritusRepository.saveAll(suoritukset).toList()
        assertTrue(savedSuoritukset.map { it.copy(id = null) }.containsAll(suoritukset))
    }

    @Test
    fun `find suoritus with search term`() {
        val suoritus = generateRandomYkiSuoritusEntity().copy(etunimet = "Ranja Testi")
        val suoritus2 =
            generateRandomYkiSuoritusEntity().copy(
                etunimet = "Testi",
                sukunimi = "Testilä",
            )
        ykiSuoritusRepository.saveAll(listOf(suoritus, suoritus2))

        val searchStr = "ranja"
        val suoritukset = ykiSuoritusRepository.find(searchStr)
        assertEquals(1, suoritukset.count())
        assertEquals(suoritus, suoritukset.first().copy(id = null))

        val anotherSuoritukset = ykiSuoritusRepository.find("testi")
        assertEquals(2, anotherSuoritukset.count())
    }

    @Test
    fun `count suoritukset`() {
        val suoritus = generateRandomYkiSuoritusEntity().copy(etunimet = "Ranja Testi")
        val suoritus2 = generateRandomYkiSuoritusEntity()
        val updatedSuoritus =
            suoritus.copy(
                lastModified = Instant.parse("2024-11-01T13:53:56Z"),
                tekstinYmmartaminen = 5,
                kirjoittaminen = 4,
                rakenteetJaSanasto = 3,
                puheenYmmartaminen = 1,
                puhuminen = 2,
                yleisarvosana = 3,
                tarkistusarvioinninSaapumisPvm = LocalDate.of(2024, 10, 1),
                tarkistusarvioinninAsiatunnus = "123123",
                tarkistusarvioidutOsakokeet = 2,
                arvosanaMuuttui = 4,
                perustelu = "Tarkistusarvioinnin testi",
                tarkistusarvioinninKasittelyPvm = LocalDate.of(2024, 10, 15),
            )
        ykiSuoritusRepository.saveAll(listOf(suoritus, suoritus2, updatedSuoritus))
        val countDistinct = ykiSuoritusRepository.countSuoritukset()
        assertEquals(2L, countDistinct, "Assert failed for count distinct suoritukset")
        val countAll = ykiSuoritusRepository.countSuoritukset(distinct = false)
        assertEquals(3L, countAll, "Assert failed for count all suoritukset")
        val countRanjaDistinct = ykiSuoritusRepository.countSuoritukset("ranja")
        assertEquals(1L, countRanjaDistinct, "Assert failed for count distinct suoritukset with a search term")
        val countRanjaAll = ykiSuoritusRepository.countSuoritukset("ranja", distinct = false)
        assertEquals(2L, countRanjaAll, "Assert failed for count all suoritukset with a search term")
    }
}
