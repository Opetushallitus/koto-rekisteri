package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Arvosana
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritus
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritusRepository
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.KielitestiSuoritusService
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.Testikieli
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.error.KielitestiErrorService
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.error.KielitestiSuoritusError
import fi.oph.kitu.kotoutumiskoulutus.suoritukset.error.KielitestiSuoritusErrorRepository
import fi.oph.kitu.oid.Oid
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class KielitestiCsvTest(
    @param:Autowired val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    @param:Autowired val kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
    @param:Autowired val kielitestiErrorService: KielitestiErrorService,
    @param:Autowired val postgres: PostgreSQLContainer,
) {
    @BeforeEach
    fun setup() {
        kielitestiSuoritusRepository.deleteAll()
        kielitestiSuoritusErrorRepository.deleteAll()
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

        val actualCsv = kielitestiErrorService.generateErrorsCsvStream()
        val expectedCsv =
            """
            virheenLuontiaika,suorittajanOid,hetu,nimi,etunimet,sukunimi,kutsumanimi,schoolOid,teacherEmail,viesti,lisatietoja,onrLisatietoja,virheellinenKentta,virheellinenArvo
            2024-11-22T10:49:49Z,,010180-9026,"Ranja Testi Öhman-Testi",Ranja,"Testi Öhman-Testi",Ranja,"1.2.246.562.10.14893989377",testi@example.com,"Kirjoitusvirhe nimessä tai henkilötunnuksessa","{""request"": {""etunimet"": ""Ranja"", ""hetu"": ""010180-9026"", ""kutsumanimi"": ""Ranja"", ""sukunimi"": ""Testi Öhman-Testi""}}","etunimet: Ranja Testi, kutsumanimi: Ranja, sukunimi: Öhman-Testi",,
            2024-11-22T10:49:49Z,"1.2.246.562.24.67409348034",010180-9026,"Eino Testi Välimaa-Testi","Eino Test",Välimaa-Testi,Eino,"1.2.246.562.10.14893989377",testi@example.com,"Unexpectedly missing quiz grade ""puhuminen"" on course ""Testaus"" for user ""1"".",,,puhuminen,"virheellinen arvosana"

            """.trimIndent()

        assertEquals(expectedCsv, actualCsv.toString(Charsets.UTF_8))
    }
}
