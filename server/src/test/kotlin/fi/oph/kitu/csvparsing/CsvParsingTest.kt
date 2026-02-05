package fi.oph.kitu.csvparsing

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.Todistuskieli
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsvResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class CsvParsingTest(
    @param:Autowired val parser: CsvParser,
    @param:Autowired private val postgres: PostgreSQLContainer<*>,
) {
    @Test
    fun `test writing csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val parser = parser.withUseHeader(true)

        val writable =
            listOf(
                YkiSuoritusCsvResponse(
                    suorittajanOID = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                    hetu = "010180-9026",
                    sukupuoli = Sukupuoli.N,
                    sukunimi = "Öhman-Testi",
                    etunimet = "Ranja Testi",
                    kansalaisuus = "EST",
                    katuosoite = "Testikuja 5",
                    postinumero = "40100",
                    postitoimipaikka = "Testilä",
                    email = "testi@testi.fi",
                    solkiTunniste = 183424,
                    lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                    tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                    tutkintokieli = Tutkintokieli.FIN,
                    tutkintotaso = Tutkintotaso.YT,
                    todistuskieli = Todistuskieli.FIN,
                    jarjestajanOID = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                    jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                    arviointitila = Arviointitila.ARVIOITU,
                    tilaLahetetty = Instant.parse("2024-10-30T14:00:00Z"),
                    arviointipaiva = LocalDate.parse("2024-11-14", dateFormatter),
                    tekstinYmmartaminen = 5,
                    kirjoittaminen = 4,
                    rakenteetJaSanasto = 3,
                    puheenYmmartaminen = 1,
                    puhuminen = 2,
                    yleisarvosana = 3,
                    tarkistusarvioinninSaapumisPvm = LocalDate.parse("2024-10-01", dateFormatter),
                    tarkistusarvioinninAsiatunnus = "123123",
                    tarkistusarvioidutOsakokeet = "3",
                    arvosanaMuuttui = "2",
                    perustelu = "Tarkistusarvioinnin testi\\nJossa rivinvaihto",
                    tarkistusarvioinninKasittelyPvm = LocalDate.parse("2024-10-15", dateFormatter),
                ),
            )
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writable)
        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,email,solkiTunniste,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,todistuskieli,jarjestajanOID,jarjestajanNimi,arviointitila,tilaLahetetty,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,puheenYmmartaminen,puhuminen,rakenteetJaSanasto,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhman-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,testi@testi.fi,183424,2024-10-30T13:53:56Z,2024-09-01,fin,YT,fin,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",ARVIOITU,2024-10-30T14:00:00Z,2024-11-14,5,4,1,2,3,3,2024-10-01,123123,3,2,"Tarkistusarvioinnin testi\nJossa rivinvaihto",2024-10-15

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }

    @Test
    fun `null values are written correctly to csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val parser = parser.withUseHeader(true)
        val writable =
            listOf(
                YkiSuoritusCsvResponse(
                    suorittajanOID = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
                    hetu = "010180-9026",
                    sukupuoli = Sukupuoli.N,
                    sukunimi = "Öhman-Testi",
                    etunimet = "Ranja Testi",
                    kansalaisuus = "EST",
                    katuosoite = "Testikuja 5",
                    postinumero = "40100",
                    postitoimipaikka = "Testilä",
                    email = null,
                    solkiTunniste = 183424,
                    lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                    tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                    tutkintokieli = Tutkintokieli.FIN,
                    tutkintotaso = Tutkintotaso.YT,
                    todistuskieli = null,
                    jarjestajanOID = Oid.parse("1.2.246.562.10.14893989377").getOrThrow(),
                    jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                    arviointitila = Arviointitila.ARVIOITAVA,
                    tilaLahetetty = null,
                    arviointipaiva = LocalDate.parse("2024-11-14", dateFormatter),
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
                ),
            )
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writable)

        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,email,solkiTunniste,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,todistuskieli,jarjestajanOID,jarjestajanNimi,arviointitila,tilaLahetetty,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,puheenYmmartaminen,puhuminen,rakenteetJaSanasto,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhman-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,,183424,2024-10-30T13:53:56Z,2024-09-01,fin,YT,,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",ARVIOITAVA,,2024-11-14,,,,,,,,,,,,

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }
}
