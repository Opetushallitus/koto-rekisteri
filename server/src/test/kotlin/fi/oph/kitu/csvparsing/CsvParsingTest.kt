package fi.oph.kitu.csvparsing

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import fi.oph.kitu.logging.MockEvent
import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import org.ietf.jgss.Oid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CsvParsingTest {
    @Test
    fun `test yki suoritukset parsing`() {
        val parser = CsvParser(MockEvent())
        val csv =
            """
            "1.2.246.562.24.20281155246","010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
            """.trimIndent()
        val suoritus = parser.convertCsvToData<YkiSuoritusCsv>(csv).first()

        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        assertEquals(Oid("1.2.246.562.24.20281155246"), suoritus.suorittajanOID)
        assertEquals("010180-9026", suoritus.hetu)
        assertEquals(Sukupuoli.N, suoritus.sukupuoli)
        assertEquals("Öhmana-Testi", suoritus.sukunimi)
        assertEquals("Ranja Testi", suoritus.etunimet)
        assertEquals("EST", suoritus.kansalaisuus)
        assertEquals("Testikuja 5", suoritus.katuosoite)
        assertEquals("40100", suoritus.postinumero)
        assertEquals("Testilä", suoritus.postitoimipaikka)
        assertEquals("testi@testi.fi", suoritus.email)
        assertEquals(183424, suoritus.suoritusID)
        assertEquals(Instant.parse("2024-10-30T13:53:56Z"), suoritus.lastModified)
        assertEquals("2024-09-01", suoritus.tutkintopaiva.format(dateFormatter))
        assertEquals(Tutkintokieli.FIN, suoritus.tutkintokieli)
        assertEquals(Tutkintotaso.YT, suoritus.tutkintotaso)
        assertEquals(Oid("1.2.246.562.10.14893989377"), suoritus.jarjestajanOID)
        assertEquals("Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus", suoritus.jarjestajanNimi)
        assertEquals("2024-11-14", suoritus.arviointipaiva.format(dateFormatter))
        assertEquals(5, suoritus.tekstinYmmartaminen)
        assertEquals(5, suoritus.kirjoittaminen)
        assertNull(suoritus.rakenteetJaSanasto)
        assertEquals(5, suoritus.puheenYmmartaminen)
        assertEquals(5, suoritus.puhuminen)
        assertNull(suoritus.yleisarvosana)
        assertNull(suoritus.tarkistusarvioinninSaapumisPvm)
        assertEquals("", suoritus.tarkistusarvioinninAsiatunnus)
        assertEquals(0, suoritus.tarkistusarvioidutOsakokeet)
        assertEquals(false, suoritus.arvosanaMuuttui)
        assertEquals("", suoritus.perustelu)
        assertNull(suoritus.tarkistusarvioinninKasittelyPvm)
    }

    @Test
    fun `test legacy language code 10 parsing`() {
        val parser = CsvParser(MockEvent())
        val arvioijaCsv =
            """
            "1.2.246.562.24.24941612410","010180-922U","Torvinen-Testi","Anniina Testi","anniina.testi@yki.fi","Testiosoite 7357","00100","HELSINKI",1994-08-01,2019-06-29,2024-06-29,0,0,"10","PT+KT"
            """.trimIndent()
        val arvioija = parser.convertCsvToData<SolkiArvioijaResponse>(arvioijaCsv)[0]
        assertEquals(Tutkintokieli.SWE10, arvioija.kieli)
    }

    @Test
    fun `test legacy language code 11 parsing`() {
        val parser = CsvParser(MockEvent())
        val arvioijaCsv =
            """
            "1.2.246.562.24.24941612410","010180-922U","Torvinen-Testi","Anniina Testi","anniina.testi@yki.fi","Testiosoite 7357","00100","HELSINKI",1994-08-01,2019-06-29,2024-06-29,0,0,"11","PT+KT"
            """.trimIndent()
        val arvioija = parser.convertCsvToData<SolkiArvioijaResponse>(arvioijaCsv)[0]
        assertEquals(Tutkintokieli.ENG11, arvioija.kieli)
    }

    @Test
    fun `test legacy language code 12 parsing`() {
        val parser = CsvParser(MockEvent())
        val arvioijaCsv =
            """
            "1.2.246.562.24.24941612410","010180-922U","Torvinen-Testi","Anniina Testi","anniina.testi@yki.fi","Testiosoite 7357","00100","HELSINKI",1994-08-01,2019-06-29,2024-06-29,0,0,"12","PT+KT"
            """.trimIndent()
        val arvioija = parser.convertCsvToData<SolkiArvioijaResponse>(arvioijaCsv)[0]
        assertEquals(Tutkintokieli.ENG12, arvioija.kieli)
    }

    @Test
    fun `parsing errors are logged `() {
        val event = MockEvent()
        val parser = CsvParser(event)
        val csv =
            """
            "INVALID_OID","010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
            "1.2.246.562.24.20281155246","INVALID_HETU","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
            "1.2.246.562.24.20281155246","010180-9026","INVALID_SEX","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
            "1.2.246.562.24.20281155246","010180-9026","N","Öhmana-Testi","Ranja Testi","INVALID_NATIONALITY","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
            """.trimIndent()

        assertThrows<RuntimeException> {
            parser.convertCsvToData<YkiSuoritusCsv>(csv)
        }

        val serializationErrors = event.keyValues.filter { kvp -> kvp.first?.startsWith("serialization.error") == true }

        val count = serializationErrors.size
        assertTrue(count == 9)

        val exceptions =
            serializationErrors
                //  Removes everything before last dot, from the keys.
                .map { kvp -> Pair(kvp.first?.substringAfterLast("."), kvp.second) }
                .filter { it.first.equals("exception") }
                .map { it.second }

        val valueInstantiationException = exceptions.first()
        val invalidFormatException = exceptions.last()

        assertTrue(valueInstantiationException is ValueInstantiationException)
        assertTrue(valueInstantiationException.message!!.contains("OID"))

        assertTrue(invalidFormatException is InvalidFormatException)
        assertTrue(invalidFormatException.message!!.contains("INVALID_SEX"))
    }

    @Test
    fun `test writing csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val parser = CsvParser(MockEvent(), useHeader = true)
        val entity =
            YkiSuoritusEntity(
                id = null,
                suorittajanOID = "1.2.246.562.24.20281155246",
                hetu = "010180-9026",
                sukupuoli = Sukupuoli.N,
                sukunimi = "Öhmana-Testi",
                etunimet = "Ranja Testi",
                kansalaisuus = "EST",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                email = "testi@testi.fi",
                suoritusId = 183424,
                lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                tutkintokieli = Tutkintokieli.FIN,
                tutkintotaso = Tutkintotaso.YT,
                jarjestajanTunnusOid = "1.2.246.562.10.14893989377",
                jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                arviointipaiva = LocalDate.parse("2024-11-14", dateFormatter),
                tekstinYmmartaminen = 5,
                kirjoittaminen = 4,
                rakenteetJaSanasto = 3,
                puheenYmmartaminen = 1,
                puhuminen = 2,
                yleisarvosana = 3,
                tarkistusarvioinninSaapumisPvm = LocalDate.parse("2024-10-01", dateFormatter),
                tarkistusarvioinninAsiatunnus = "123123",
                tarkistusarvioidutOsakokeet = 2,
                arvosanaMuuttui = true,
                perustelu = "Tarkistusarvioinnin testi",
                tarkistusarvioinninKasittelyPvm = LocalDate.parse("2024-10-15", dateFormatter),
            )
        val writable = listOf(entity.toYkiSuoritusCsv())
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writable)
        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,email,suoritusID,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,jarjestajanOID,jarjestajanNimi,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,rakenteetJaSanasto,puheenYmmartaminen,puhuminen,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhmana-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,testi@testi.fi,183424,2024-10-30T13:53:56Z,2024-09-01,FIN,YT,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,4,3,1,2,3,2024-10-01,123123,2,true,"Tarkistusarvioinnin testi",2024-10-15

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }

    @Test
    fun `null values are written correctly to csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
        val parser = CsvParser(MockEvent(), useHeader = true)
        val entity =
            YkiSuoritusEntity(
                id = null,
                suorittajanOID = "1.2.246.562.24.20281155246",
                hetu = "010180-9026",
                sukupuoli = Sukupuoli.N,
                sukunimi = "Öhmana-Testi",
                etunimet = "Ranja Testi",
                kansalaisuus = "EST",
                katuosoite = "Testikuja 5",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                email = null,
                suoritusId = 183424,
                lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                tutkintopaiva = LocalDate.parse("2024-09-01", dateFormatter),
                tutkintokieli = Tutkintokieli.FIN,
                tutkintotaso = Tutkintotaso.YT,
                jarjestajanTunnusOid = "1.2.246.562.10.14893989377",
                jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
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
            )
        val writable = listOf(entity.toYkiSuoritusCsv())
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writable)

        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,email,suoritusID,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,jarjestajanOID,jarjestajanNimi,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,rakenteetJaSanasto,puheenYmmartaminen,puhuminen,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhmana-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,,183424,2024-10-30T13:53:56Z,2024-09-01,FIN,YT,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,,,,,,,,,,,,

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }
}
