package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.arvioijat.YkiArvioijaTila
import fi.oph.kitu.yki.arvioijat.YkiArviointioikeusEntity
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull
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
    fun `Uuden arviointioikeuden tallennus olemassaolevalle henkilölle lisää sen kyseiselle henkilölle`() {
        val sweArviointioikeus =
            YkiArviointioikeusEntity(
                id = null,
                arvioijaId = null,
                kaudenAlkupaiva = null,
                kaudenPaattymispaiva = null,
                jatkorekisterointi = false,
                tila = YkiArvioijaTila.AKTIIVINEN,
                kieli = Tutkintokieli.SWE,
                tasot = setOf(Tutkintotaso.YT),
                ensimmainenRekisterointipaiva = LocalDate.now(),
                rekisteriintuontiaika = null,
            )
        val engArviointioikeus = sweArviointioikeus.copy(kieli = Tutkintokieli.ENG)

        val arvioija =
            YkiArvioijaEntity(
                id = null,
                arvioijanOppijanumero = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                henkilotunnus = "010180-9026",
                sukunimi = "Öhman-Testi",
                etunimet = "Ranja Testi",
                sahkopostiosoite = "testi@testi.fi",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                arviointioikeudet = listOf(sweArviointioikeus),
            )
        arvioijaRepository.upsert(arvioija)

        val arvioijaEng = arvioija.copy(arviointioikeudet = listOf(engArviointioikeus))
        val savedId = arvioijaRepository.upsert(arvioijaEng)
        val saved = arvioijaRepository.findById(savedId).getOrNull()

        assertEquals(
            arvioija.copy(arviointioikeudet = listOf(sweArviointioikeus, engArviointioikeus)),
            saved?.copy(
                id = null,
                arviointioikeudet =
                    saved.arviointioikeudet.map { it.copy(id = null, arvioijaId = null, rekisteriintuontiaika = null) },
            ),
        )

        val allArvioijat = arvioijaRepository.findAll()
        assertEquals(1, allArvioijat.count())
    }

    @Test
    fun `Duplikaatteja ei tallenneta`() {
        val arvioija =
            YkiArvioijaEntity(
                id = null,
                arvioijanOppijanumero = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                henkilotunnus = "010180-9026",
                sukunimi = "Öhman-Testi",
                etunimet = "Ranja Testi",
                sahkopostiosoite = "testi@testi.fi",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                arviointioikeudet =
                    listOf(
                        YkiArviointioikeusEntity(
                            id = null,
                            arvioijaId = null,
                            kaudenAlkupaiva = null,
                            kaudenPaattymispaiva = null,
                            jatkorekisterointi = false,
                            tila = YkiArvioijaTila.AKTIIVINEN,
                            kieli = Tutkintokieli.SWE,
                            tasot = setOf(Tutkintotaso.YT),
                            ensimmainenRekisterointipaiva = LocalDate.now(),
                            rekisteriintuontiaika = null,
                        ),
                    ),
            )

        arvioijaRepository.saveAllNewEntities(listOf(arvioija))
        val arvioijat = arvioijaRepository.findAll()
        assertEquals(1, arvioijat.count())
        assertEquals(1, arvioijat.sumOf { it.arviointioikeudet.size })

        arvioijaRepository.saveAllNewEntities(listOf(arvioija))
        val updatedArvioijat = arvioijaRepository.findAll()
        assertEquals(1, updatedArvioijat.count())
        assertEquals(1, updatedArvioijat.sumOf { it.arviointioikeudet.size })
    }

    @Test
    fun `different versions of the same arvioija are saved`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val arvioija =
            YkiArvioijaEntity(
                id = null,
                arvioijanOppijanumero = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                henkilotunnus = "010180-9026",
                sukunimi = "Öhman-Testi",
                etunimet = "Ranja Testi",
                sahkopostiosoite = "testi@testi.fi",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                arviointioikeudet =
                    listOf(
                        YkiArviointioikeusEntity(
                            id = null,
                            arvioijaId = null,
                            kaudenAlkupaiva = LocalDate.parse("2024-09-01", dateFormatter),
                            kaudenPaattymispaiva = LocalDate.parse("2025-09-01", dateFormatter),
                            jatkorekisterointi = false,
                            tila = YkiArvioijaTila.AKTIIVINEN,
                            kieli = Tutkintokieli.SWE,
                            tasot = setOf(Tutkintotaso.YT),
                            ensimmainenRekisterointipaiva = LocalDate.parse("2024-09-01", dateFormatter),
                            rekisteriintuontiaika = null,
                        ),
                    ),
            )

        arvioijaRepository.saveAllNewEntities(listOf(arvioija))
        val arvioijat = arvioijaRepository.findAll()
        assertEquals(1, arvioijat.count())

        val updatedArvioija =
            arvioija.copy(
                arviointioikeudet =
                    arvioija.arviointioikeudet.map {
                        it.copy(kaudenAlkupaiva = LocalDate.now(), jatkorekisterointi = true)
                    },
            )
        val savedIds = arvioijaRepository.saveAllNewEntities(listOf(updatedArvioija))
        val saved = arvioijaRepository.findById(savedIds.first()).getOrNull()
        assertEquals(1, savedIds.count())
        assertEquals(
            updatedArvioija,
            saved?.copy(
                id = null,
                arviointioikeudet =
                    saved.arviointioikeudet.map {
                        it.copy(id = null, arvioijaId = null, rekisteriintuontiaika = null)
                    },
            ),
        )
        val updatedArvioijat = arvioijaRepository.findAll()
        assertEquals(1, updatedArvioijat.count())
    }
}
