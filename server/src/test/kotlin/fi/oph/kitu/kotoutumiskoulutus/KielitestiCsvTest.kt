package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.csvparsing.CsvParser
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class KielitestiCsvTest(
    @param:Autowired val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    @param:Autowired val kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
    @param:Autowired val koealustaService: KoealustaService,
    @param:Autowired val csvParser: CsvParser,
    @param:Autowired val postgres: PostgreSQLContainer<*>,
) {
    @BeforeEach
    fun setup() {
        kielitestiSuoritusRepository.deleteAll()
        kielitestiSuoritusErrorRepository.deleteAll()
    }

    @Test
    fun `Kielitestin suoritukset kaantyvat csv-tiedostoksi oikein`() {
        val suoritukset =
            listOf(
                KielitestiSuoritus(
                    id = null,
                    firstNames = "Eka",
                    lastName = "Ensiö",
                    preferredname = "Eka",
                    oppijanumero = Oid.parse("1.2.246.562.10.1234567890").getOrThrow(),
                    email = "eka@fakeemail.net",
                    timeCompleted = Instant.ofEpochSecond(1761036997),
                    schoolOid = Oid.parse("1.2.246.562.10.77255241653").getOrThrow(),
                    teacherEmail = "ope@fakeemail.net",
                    courseid = 52,
                    coursename = "Yksikkötesti",
                    luetunYmmartaminenResult = "A1",
                    kuullunYmmartaminenResult = "A2",
                    puheResult = "A1",
                    kirjoittaminenResult = "Alle A1",
                ),
                KielitestiSuoritus(
                    id = null,
                    firstNames = "Toka",
                    lastName = "Toisio",
                    preferredname = "Toka",
                    oppijanumero = Oid.parse("1.2.246.562.10.303909808").getOrThrow(),
                    email = "toka@fakeemail.net",
                    timeCompleted = Instant.ofEpochSecond(1761036997),
                    schoolOid = Oid.parse("1.2.246.562.10.77255241653").getOrThrow(),
                    teacherEmail = "ope@fakeemail.net",
                    courseid = 52,
                    coursename = "Yksikkötesti",
                    luetunYmmartaminenResult = "A1",
                    kuullunYmmartaminenResult = "A2",
                    puheResult = "A1",
                    kirjoittaminenResult = "Alle A1",
                ),
            )

        kielitestiSuoritusRepository.saveAll(suoritukset)

        val actualCsv = koealustaService.generateSuorituksetCsvStream()
        val expectedCsv =
            """
            sukunimi,etunimet,sahkoposti,kurssinNimi,suoritusaika,oppijanumero,luetunYmmartaminen,kuullunYmmartaminen,puhuminen,kirjoittaminen
            Ensiö,Eka,eka@fakeemail.net,Yksikkötesti,2025-10-21T08:56:37Z,"1.2.246.562.10.1234567890",A1,A2,A1,"Alle A1"
            Toisio,Toka,toka@fakeemail.net,Yksikkötesti,2025-10-21T08:56:37Z,1.2.246.562.10.303909808,A1,A2,A1,"Alle A1"
            
            """.trimIndent()

        assertEquals(expectedCsv, actualCsv.toString(Charsets.UTF_8))
    }

    @Test
    fun `Kielitestin virheet kaantyvat csv-tiedostoksi oikein`() {
        val suoritukset =
            listOf(
                KielitestiSuoritusError(
                    id = null,
                    suorittajanOid = null,
                    hetu = "010180-9026",
                    nimi = "Ranja Testi Öhman-Testi",
                    etunimet = "Ranja",
                    sukunimi = "Testi Öhman-Testi",
                    kutsumanimi = "Ranja",
                    schoolOid = Oid.parse("1.2.246.562.10.14893989377").getOrNull(),
                    teacherEmail = "testi@example.com",
                    virheenLuontiaika = Instant.parse("2024-11-22T10:49:49Z"),
                    viesti = "",
                    virheellinenKentta = null,
                    virheellinenArvo = null,
                    lisatietoja = "",
                    onrLisatietoja = "etunimet: Ranja Testi, kutsumanimi: Ranja, sukunimi: Öhman-Testi",
                ),
            )

        kielitestiSuoritusErrorRepository.saveAll(suoritukset)

        val actualCsv = koealustaService.generateErrorsCsvStream()
        val expectedCsv =
            """
            virheenLuontiaika,suorittajanOid,hetu,nimi,etunimet,sukunimi,kutsumanimi,schoolOid,teacherEmail,viesti,lisatietoja,onrLisatietoja,virheellinenKentta,virheellinenArvo
            2024-11-22T10:49:49Z,,010180-9026,"Ranja Testi Öhman-Testi",Ranja,"Testi Öhman-Testi",Ranja,"1.2.246.562.10.14893989377",testi@example.com,,,"etunimet: Ranja Testi, kutsumanimi: Ranja, sukunimi: Öhman-Testi",,

            """.trimIndent()

        assertEquals(expectedCsv, actualCsv.toString(Charsets.UTF_8))
    }
}
