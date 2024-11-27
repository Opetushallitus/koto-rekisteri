package fi.oph.kitu.csvparsing

import fi.oph.kitu.yki.Sukupuoli
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import fi.oph.kitu.yki.suoritukset.SolkiSuoritusResponse
import org.ietf.jgss.Oid
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CsvParsingTest {
    @Test
    fun `test yki suoritukset parsing`() {
        val csv =
            """
            "1.2.246.562.24.20281155246","010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
            """.trimIndent()
        val suoritus = csv.asCsv<SolkiSuoritusResponse>()[0]
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = SimpleDateFormat(datePattern)
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
        assertEquals("2024-09-01", dateFormatter.format(suoritus.tutkintopaiva))
        assertEquals(Tutkintokieli.FIN, suoritus.tutkintokieli)
        assertEquals(Tutkintotaso.YT, suoritus.tutkintotaso)
        assertEquals(Oid("1.2.246.562.10.14893989377"), suoritus.jarjestajanOID)
        assertEquals("Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus", suoritus.jarjestajanNimi)
        assertEquals("2024-11-14", dateFormatter.format(suoritus.arviointipaiva))
        assertEquals(5.0, suoritus.tekstinYmmartaminen)
        assertEquals(5.0, suoritus.kirjoittaminen)
        assertNull(suoritus.rakenteetJaSanasto)
        assertEquals(5.0, suoritus.puheenYmmartaminen)
        assertEquals(5.0, suoritus.puhuminen)
        assertNull(suoritus.yleisarvosana)
        assertNull(suoritus.tarkistusarvioinninSaapumisPvm)
        assertEquals("", suoritus.tarkistusarvioinninAsiatunnus)
        assertEquals(0, suoritus.tarkistusarvioidutOsakokeet)
        assertEquals(0, suoritus.arvosanaMuuttui)
        assertEquals("", suoritus.perustelu)
        assertNull(suoritus.tarkistusarvioinninKasittelyPvm)
    }
}
