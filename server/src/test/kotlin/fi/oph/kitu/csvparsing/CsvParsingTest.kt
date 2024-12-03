package fi.oph.kitu.csvparsing

import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import org.ietf.jgss.Oid
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CsvParsingTest {
    @Test
    fun `test yki suoritukset parsing`() {
        val csv =
            """
            "1.2.246.562.24.20281155246","010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
            """.trimIndent()
        val suoritus = csv.asCsv<YkiSuoritusCsv>()[0]
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
        assertEquals(5.0, suoritus.tekstinYmmartaminen)
        assertEquals(5.0, suoritus.kirjoittaminen)
        assertNull(suoritus.rakenteetJaSanasto)
        assertEquals(5.0, suoritus.puheenYmmartaminen)
        assertEquals(5.0, suoritus.puhuminen)
        assertNull(suoritus.yleisarvosana)
        assertNull(suoritus.tarkistusarvioinninSaapumisPvm)
        assertEquals("", suoritus.tarkistusarvioinninAsiatunnus)
        assertEquals(0, suoritus.tarkistusarvioidutOsakokeet)
        assertEquals(false, suoritus.arvosanaMuuttui)
        assertEquals("", suoritus.perustelu)
        assertNull(suoritus.tarkistusarvioinninKasittelyPvm)
    }

    @Test
    fun `test writing csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
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
                tekstinYmmartaminen = 5.0,
                kirjoittaminen = 4.0,
                rakenteetJaSanasto = 3.0,
                puheenYmmartaminen = 1.0,
                puhuminen = 2.0,
                yleisarvosana = 3.0,
                tarkistusarvioinninSaapumisPvm = LocalDate.parse("2024-10-01", dateFormatter),
                tarkistusarvioinninAsiatunnus = "123123",
                tarkistusarvioidutOsakokeet = 2,
                arvosanaMuuttui = true,
                perustelu = "Tarkistusarvioinnin testi",
                tarkistusarvioinninKasittelyPvm = LocalDate.parse("2024-10-15", dateFormatter),
            )
        val writable = listOf(entity.toYkiSuoritusCsv())
        val outputStream = ByteArrayOutputStream()
        writable.writeAsCsv(outputStream, CsvArgs(useHeader = true))
        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,email,suoritusID,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,jarjestajanOID,jarjestajanNimi,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,rakenteetJaSanasto,puheenYmmartaminen,puhuminen,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhmana-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,testi@testi.fi,183424,2024-10-30T13:53:56Z,2024-09-01,FIN,YT,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5.0,4.0,3.0,1.0,2.0,3.0,2024-10-01,123123,2,true,"Tarkistusarvioinnin testi",2024-10-15

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }

    @Test
    fun `null values are written correctly to csv`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
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
        writable.writeAsCsv(outputStream, CsvArgs(useHeader = true))
        val expectedCsv =
            """
            suorittajanOID,hetu,sukupuoli,sukunimi,etunimet,kansalaisuus,katuosoite,postinumero,postitoimipaikka,email,suoritusID,lastModified,tutkintopaiva,tutkintokieli,tutkintotaso,jarjestajanOID,jarjestajanNimi,arviointipaiva,tekstinYmmartaminen,kirjoittaminen,rakenteetJaSanasto,puheenYmmartaminen,puhuminen,yleisarvosana,"tarkistusarvioinninSaapumisPvm","tarkistusarvioinninAsiatunnus","tarkistusarvioidutOsakokeet",arvosanaMuuttui,perustelu,"tarkistusarvioinninKasittelyPvm"
            "1.2.246.562.24.20281155246",010180-9026,N,Öhmana-Testi,"Ranja Testi",EST,"Testikuja 5",40100,Testilä,,183424,2024-10-30T13:53:56Z,2024-09-01,FIN,YT,"1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,,,,,,,,,,,,

            """.trimIndent()
        assertEquals(expectedCsv, outputStream.toString(Charsets.UTF_8))
    }
}
