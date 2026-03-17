package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.equalsIgnoringAnnotated
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class)
class KielitestiSuoritusRepositoryTest(
    @param:Autowired private val postgres: PostgreSQLContainer<*>,
    @param:Autowired private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    @param:Autowired private val customKielitestiSuoritusRepository: CustomKielitestiSuoritusRepository,
) {
    private val suoritusRaija =
        KielitestiSuoritus(
            etunimet = "Raija Testi",
            sukunimi = "Öhman-Testi",
            kutsumanimi = "Ranja",
            oppijanumero = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
            email = "ranja@example.com",
            suoritusaika = Instant.parse("2024-11-22T10:49:49Z"),
            oppilaitosOid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
            opettajanEmail = "ope@example.com",
            kurssiId = 32,
            kurssi = "Integraatio testaus",
            luetunYmmartaminen = Arvosana.A1,
            kuullunYmmartaminen = Arvosana.B1,
            puhe = Arvosana.B1,
            kirjoittaminen = Arvosana.A2,
        )

    private val suoritusJennika =
        KielitestiSuoritus(
            etunimet = "Kukka-Maaria Jennika Etel",
            sukunimi = "Haapakoski Henriksson",
            kutsumanimi = "Jennika",
            oppijanumero = Oid.parse("1.2.246.562.198.88975028874").getOrThrow(),
            email = "jennika@example.com",
            suoritusaika = Instant.parse("2025-05-27T10:44:19Z"),
            oppilaitosOid = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
            opettajanEmail = "ope@example.com",
            kurssiId = 32,
            kurssi = "Integraatio testaus",
            luetunYmmartaminen = Arvosana.A1,
            kuullunYmmartaminen = Arvosana.B1,
            puhe = Arvosana.B1,
            kirjoittaminen = Arvosana.A2,
        )

    @BeforeEach
    fun nukeDB() {
        kielitestiSuoritusRepository.deleteAll()
        kielitestiSuoritusRepository.saveAll(listOf(suoritusRaija, suoritusJennika))
    }

    @Test
    fun `Kielitesti suoritusten haku etunimellä`() {
        val result = customKielitestiSuoritusRepository.findSuoritukset("ranja")
        assertEquals(1, result.size)
        assertTrue(suoritusRaija.equalsIgnoringAnnotated(result.first(), "KOTO"))
    }

    @Test
    fun `Kielitestisuoritusten haku etu- ja sukunimellä`() {
        val result = customKielitestiSuoritusRepository.findSuoritukset("ranja öhman")
        assertEquals(1, result.size)
        assertTrue(suoritusRaija.equalsIgnoringAnnotated(result.first(), "KOTO"))
    }

    @Test
    fun `Kielitestisuorituksen haku oppijanumerolla`() {
        val result = customKielitestiSuoritusRepository.findSuoritukset("1.2.246.562.198.88975028874")
        assertEquals(1, result.size)
        assertTrue(suoritusJennika.equalsIgnoringAnnotated(result.first(), "KOTO"))
    }

    @Test
    fun `Kielitestisuorituksen haku kurssin nimellä`() {
        val result = customKielitestiSuoritusRepository.findSuoritukset("Integraatio testaus")
        assertEquals(2, result.size)
    }

    @Test
    fun `Kielitestisuorituksen haku tyhjällä hakusanalla`() {
        val result = customKielitestiSuoritusRepository.findSuoritukset(null)
        assertEquals(2, result.size)
    }

    @Test
    fun `duplikaattisuoritus tunnistetaan`() {
        val duplicateSuoritus = suoritusRaija.copy(lastModified = Instant.now())
        val result = customKielitestiSuoritusRepository.exists(duplicateSuoritus)
        assertTrue(result)
    }

    @Test
    fun `Päivitettyä suoritusta ei tulkita duplikaatiksi`() {
        val updatedSuoritus =
            suoritusRaija.copy(
                kuullunYmmartaminen = Arvosana.YLIB1,
                lastModified = Instant.now(),
            )
        val result = customKielitestiSuoritusRepository.exists(updatedSuoritus)
        assertFalse(result)
    }

    @Test
    fun `findSuoritukset palauttaa vain uusimmat versiot päivitetyistä suorituksista`() {
        val updatedSuoritusRaija =
            suoritusRaija.copy(
                kuullunYmmartaminen = Arvosana.YLIB1,
                lastModified = Instant.now(),
            )
        val updatedsuoritusJennika =
            suoritusJennika.copy(
                kuullunYmmartaminen = Arvosana.YLIB1,
                puhe = Arvosana.YLIB1,
                lastModified = Instant.now(),
            )
        kielitestiSuoritusRepository.saveAll(listOf(updatedSuoritusRaija, updatedsuoritusJennika))
        val suoritukset = customKielitestiSuoritusRepository.findSuoritukset()
        val resultRaija = suoritukset.find { it.oppijanumero == updatedSuoritusRaija.oppijanumero }
        val resultJennika = suoritukset.find { it.oppijanumero == updatedsuoritusJennika.oppijanumero }
        assertAll(
            fun () = assertEquals(2, suoritukset.size),
            fun () = assertEquals(Arvosana.YLIB1, resultRaija?.kuullunYmmartaminen),
            fun () = assertEquals(Arvosana.YLIB1, resultJennika?.kuullunYmmartaminen),
            fun () = assertEquals(Arvosana.YLIB1, resultJennika?.puhe),
        )
    }
}
