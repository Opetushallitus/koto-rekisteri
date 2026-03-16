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
                    etunimet = "Eka",
                    sukunimi = "Ensiö",
                    kutsumanimi = "Eka",
                    oppijanumero = Oid.parse("1.2.246.562.10.1234567890").getOrThrow(),
                    email = "eka@fakeemail.net",
                    suoritusaika = Instant.ofEpochSecond(1761036997),
                    oppilaitosOid = Oid.parse("1.2.246.562.10.77255241653").getOrThrow(),
                    opettajanEmail = "ope@fakeemail.net",
                    kurssiId = 52,
                    kurssi = "Yksikkötesti",
                    luetunYmmartaminen = Arvosana.A1,
                    kuullunYmmartaminen = Arvosana.A2,
                    puhe = Arvosana.A1,
                    kirjoittaminen = Arvosana.ALLEA1,
                    testikieli = Testikieli.FIN,
                ),
                KielitestiSuoritus(
                    id = null,
                    etunimet = "Toka",
                    sukunimi = "Toisio",
                    kutsumanimi = "Toka",
                    oppijanumero = Oid.parse("1.2.246.562.10.303909808").getOrThrow(),
                    email = "toka@fakeemail.net",
                    suoritusaika = Instant.ofEpochSecond(1761036997),
                    oppilaitosOid = Oid.parse("1.2.246.562.10.77255241653").getOrThrow(),
                    opettajanEmail = "ope@fakeemail.net",
                    kurssiId = 52,
                    kurssi = "Yksikkötesti",
                    luetunYmmartaminen = Arvosana.A1,
                    kuullunYmmartaminen = Arvosana.A2,
                    puhe = Arvosana.A1,
                    kirjoittaminen = Arvosana.ALLEA1,
                    testikieli = Testikieli.SWE,
                ),
            )

        kielitestiSuoritusRepository.saveAll(suoritukset)

        val actualCsv = koealustaService.generateSuorituksetCsvStream()
        val expectedCsv =
            """
            oppijanumero,sukunimi,etunimet,kutsumanimi,sahkoposti,kurssiId,kurssinNimi,testikieli,organisaatioOid,organisaatio,suoritusaika,luetunYmmartaminen,kuullunYmmartaminen,puhuminen,kirjoittaminen
            "1.2.246.562.10.1234567890",Ensiö,Eka,Eka,eka@fakeemail.net,52,Yksikkötesti,FIN,"1.2.246.562.10.77255241653",,2025-10-21T08:56:37Z,A1,A2,A1,"Alle A1"
            1.2.246.562.10.303909808,Toisio,Toka,Toka,toka@fakeemail.net,52,Yksikkötesti,SWE,"1.2.246.562.10.77255241653",,2025-10-21T08:56:37Z,A1,A2,A1,"Alle A1"
            
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
                    viesti = "Kirjoitusvirhe nimessä tai henkilötunnuksessa",
                    virheellinenKentta = null,
                    virheellinenArvo = null,
                    lisatietoja =
                        """
                        {"request": {"etunimet": "Ranja", "hetu": "010180-9026", "kutsumanimi": "Ranja", "sukunimi": "Testi Öhman-Testi"}}
                        """.trimIndent(),
                    onrLisatietoja = "etunimet: Ranja Testi, kutsumanimi: Ranja, sukunimi: Öhman-Testi",
                ),
                KielitestiSuoritusError(
                    id = null,
                    suorittajanOid = "1.2.246.562.24.67409348034",
                    hetu = "010180-9026",
                    nimi = "Eino Testi Välimaa-Testi",
                    etunimet = "Eino Test",
                    sukunimi = "Välimaa-Testi",
                    kutsumanimi = "Eino",
                    schoolOid = Oid.parse("1.2.246.562.10.14893989377").getOrNull(),
                    teacherEmail = "testi@example.com",
                    virheenLuontiaika = Instant.parse("2024-11-22T10:49:49Z"),
                    viesti = "Unexpectedly missing quiz grade \"puhuminen\" on course \"Testaus\" for user \"1\".",
                    virheellinenKentta = "puhuminen",
                    virheellinenArvo = "virheellinen arvosana",
                    lisatietoja = null,
                    onrLisatietoja = null,
                ),
            )

        kielitestiSuoritusErrorRepository.saveAll(suoritukset)

        val actualCsv = koealustaService.generateErrorsCsvStream()
        val expectedCsv =
            """
            virheenLuontiaika,suorittajanOid,hetu,nimi,etunimet,sukunimi,kutsumanimi,schoolOid,teacherEmail,viesti,lisatietoja,onrLisatietoja,virheellinenKentta,virheellinenArvo
            2024-11-22T10:49:49Z,,010180-9026,"Ranja Testi Öhman-Testi",Ranja,"Testi Öhman-Testi",Ranja,"1.2.246.562.10.14893989377",testi@example.com,"Kirjoitusvirhe nimessä tai henkilötunnuksessa","{""request"": {""etunimet"": ""Ranja"", ""hetu"": ""010180-9026"", ""kutsumanimi"": ""Ranja"", ""sukunimi"": ""Testi Öhman-Testi""}}","etunimet: Ranja Testi, kutsumanimi: Ranja, sukunimi: Öhman-Testi",,
            2024-11-22T10:49:49Z,"1.2.246.562.24.67409348034",010180-9026,"Eino Testi Välimaa-Testi","Eino Test",Välimaa-Testi,Eino,"1.2.246.562.10.14893989377",testi@example.com,"Unexpectedly missing quiz grade ""puhuminen"" on course ""Testaus"" for user ""1"".",,,puhuminen,"virheellinen arvosana"

            """.trimIndent()

        assertEquals(expectedCsv, actualCsv.toString(Charsets.UTF_8))
    }
}
