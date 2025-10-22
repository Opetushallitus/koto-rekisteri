package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.YkiArvioijaTila
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YkiArvioijaRepositoryTest(
    @param:Autowired private val postgres: PostgreSQLContainer<*>,
    @param:Autowired private val arvioijaRepository: YkiArvioijaRepository,
) {
    @BeforeEach
    fun nukeDb() {
        arvioijaRepository.deleteAll()
    }

    @Test
    fun `saveAll returns only the saved arvioijat`() {
        val arvioija =
            YkiArvioijaEntity(
                id = null,
                rekisteriintuontiaika = null,
                arvioijanOppijanumero = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
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
                tila = YkiArvioijaTila.AKTIIVINEN,
                kieli = Tutkintokieli.SWE,
                tasot = setOf(Tutkintotaso.YT),
            )
        arvioijaRepository.saveAllNewEntities(listOf(arvioija))

        val arvioijaEng = arvioija.copy(kieli = Tutkintokieli.ENG)
        val saved = arvioijaRepository.saveAllNewEntities(listOf(arvioijaEng))
        assertEquals(1, saved.count())
        assertEquals(arvioijaEng, saved.elementAt(0).copy(id = null, rekisteriintuontiaika = null))

        val allArvioijat = arvioijaRepository.findAll()
        assertEquals(2, allArvioijat.count())
    }

    @Test
    fun `arvioija duplicate is not saved`() {
        val arvioija =
            YkiArvioijaEntity(
                id = null,
                rekisteriintuontiaika = null,
                arvioijanOppijanumero = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
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
                tila = YkiArvioijaTila.AKTIIVINEN,
                kieli = Tutkintokieli.SWE,
                tasot = setOf(Tutkintotaso.YT),
            )

        val initialSaved = arvioijaRepository.saveAllNewEntities(listOf(arvioija))
        val arvioijat = arvioijaRepository.findAll()
        assertEquals(1, arvioijat.count())
        assertEquals(1, initialSaved.count())

        val saved = arvioijaRepository.saveAllNewEntities(listOf(arvioija))
        val updatedArvioijat = arvioijaRepository.findAll()
        assertEquals(1, updatedArvioijat.count())
        assertEquals(0, saved.count())
    }

    @Test
    fun `different versions of the same arvioija are saved`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val arvioija =
            YkiArvioijaEntity(
                id = null,
                rekisteriintuontiaika = null,
                arvioijanOppijanumero = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                henkilotunnus = "010180-9026",
                sukunimi = "Öhman-Testi",
                etunimet = "Ranja Testi",
                sahkopostiosoite = "testi@testi.fi",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                ensimmainenRekisterointipaiva = LocalDate.parse("2024-09-01", dateFormatter),
                kaudenAlkupaiva = LocalDate.parse("2024-09-01", dateFormatter),
                kaudenPaattymispaiva = LocalDate.parse("2025-09-01", dateFormatter),
                jatkorekisterointi = false,
                tila = YkiArvioijaTila.AKTIIVINEN,
                kieli = Tutkintokieli.SWE,
                tasot = setOf(Tutkintotaso.YT),
            )

        arvioijaRepository.saveAllNewEntities(listOf(arvioija))
        val arvioijat = arvioijaRepository.findAll()
        assertEquals(1, arvioijat.count())

        val updatedArvioija = arvioija.copy(kaudenAlkupaiva = LocalDate.now(), jatkorekisterointi = true)
        val saved = arvioijaRepository.saveAllNewEntities(listOf(updatedArvioija))
        assertEquals(1, saved.count())
        assertEquals(updatedArvioija, saved.elementAt(0).copy(id = null, rekisteriintuontiaika = null))
        val updatedArvioijat = arvioijaRepository.findAll()
        assertEquals(2, updatedArvioijat.count())
    }
}
