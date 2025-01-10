package fi.oph.kitu.yki

import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
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
class YkiArvioijaRepositoryTest(
    @Autowired private val arvioijaRepository: YkiArvioijaRepository,
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
        arvioijaRepository.deleteAll()
    }

    @Test
    fun `arvioija duplicate is not saved`() {
        val arvioija =
            YkiArvioijaEntity(
                id = null,
                rekisteriintuontiaika = null,
                arvioijanOppijanumero = "1.2.246.562.24.20281155246",
                henkilotunnus = "010180-9026",
                sukunimi = "Öhman-Testi",
                etunimet = "Ranja Testi",
                sahkopostiosoite = "testi@testi.fi",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                ensimmainenRekisterointipaiva = LocalDate.now(),
                kaudenAlkupaiva = null,
                kaudenPaattymispaiva = null,
                jatkorekisterointi = false,
                tila = 0,
                kieli = Tutkintokieli.SWE,
                tasot = setOf(Tutkintotaso.YT),
            )

        arvioijaRepository.saveAll(listOf(arvioija))
        val arvioijat = arvioijaRepository.findAll().toList()
        assertEquals(1, arvioijat.count())

        arvioijaRepository.saveAll(listOf(arvioija))
        val updatedArvioijat = arvioijaRepository.findAll().toList()
        assertEquals(1, updatedArvioijat.count())
    }

    @Test
    fun `different versions of the same arvioija are saved`() {
        val arvioija =
            YkiArvioijaEntity(
                id = null,
                rekisteriintuontiaika = null,
                arvioijanOppijanumero = "1.2.246.562.24.20281155246",
                henkilotunnus = "010180-9026",
                sukunimi = "Öhman-Testi",
                etunimet = "Ranja Testi",
                sahkopostiosoite = "testi@testi.fi",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                ensimmainenRekisterointipaiva = LocalDate.now(),
                kaudenAlkupaiva = null,
                kaudenPaattymispaiva = null,
                jatkorekisterointi = false,
                tila = 0,
                kieli = Tutkintokieli.SWE,
                tasot = setOf(Tutkintotaso.YT),
            )

        arvioijaRepository.saveAll(listOf(arvioija))
        val arvioijat = arvioijaRepository.findAll().toList()
        assertEquals(1, arvioijat.count())

        val updatedArvioija = arvioija.copy(kaudenAlkupaiva = LocalDate.now(), jatkorekisterointi = true)
        arvioijaRepository.saveAll(listOf(updatedArvioija))
        val updatedArvioijat = arvioijaRepository.findAll().toList()
        assertEquals(2, updatedArvioijat.count())
    }
}
