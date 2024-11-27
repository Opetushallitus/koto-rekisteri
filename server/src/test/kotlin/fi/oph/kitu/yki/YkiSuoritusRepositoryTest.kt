package fi.oph.kitu.yki

import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.text.SimpleDateFormat
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Testcontainers
class YkiSuoritusRepositoryTest(
    @Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
) {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")
    }

    @BeforeEach
    fun nukeDb() {
        ykiSuoritusRepository.deleteAll()
    }

    @Test
    fun `suoritus is saved correctly`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = SimpleDateFormat(datePattern)
        val suoritus =
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
                tutkintopaiva = dateFormatter.parse("2024-09-01"),
                tutkintokieli = Tutkintokieli.FIN,
                tutkintotaso = Tutkintotaso.YT,
                jarjestajanTunnusOid = "1.2.246.562.10.14893989377",
                jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                arviointipaiva = dateFormatter.parse("2024-11-14"),
                tekstinYmmartaminen = 5,
                kirjoittaminen = 4,
                rakenteetJaSanasto = 3,
                puheenYmmartaminen = 1,
                puhuminen = 2,
                yleisarvosana = 3,
                tarkistusarvioinninSaapumisPvm = dateFormatter.parse("2024-10-01"),
                tarkistusarvioinninAsiatunnus = "123123",
                tarkistusarvioidutOsakokeet = 2,
                arvosanaMuuttui = true,
                perustelu = "Tarkistusarvioinnin testi",
                tarkistusarvioinninKasittelyPvm = dateFormatter.parse("2024-10-15"),
            )
        val savedSuoritukset = ykiSuoritusRepository.saveAll(listOf(suoritus)).toList()
        assertEquals(suoritus, savedSuoritukset[0].copy(id = null))
    }

    @Test
    fun `suoritus with null values is saved correctly`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = SimpleDateFormat(datePattern)
        val suoritus =
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
                tutkintopaiva = dateFormatter.parse("2024-09-01"),
                tutkintokieli = Tutkintokieli.FIN,
                tutkintotaso = Tutkintotaso.YT,
                jarjestajanTunnusOid = "1.2.246.562.10.14893989377",
                jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                arviointipaiva = dateFormatter.parse("2024-11-14"),
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
        val savedSuoritukset = ykiSuoritusRepository.saveAll(listOf(suoritus)).toList()
        assertEquals(suoritus, savedSuoritukset[0].copy(id = null))
    }

    @Test
    fun `findAllDistinct returns the latest suoritus of same suoritusId`() {
        val datePattern = "yyyy-MM-dd"
        val dateFormatter = SimpleDateFormat(datePattern)
        val suoritus =
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
                tutkintopaiva = dateFormatter.parse("2024-09-01"),
                tutkintokieli = Tutkintokieli.FIN,
                tutkintotaso = Tutkintotaso.YT,
                jarjestajanTunnusOid = "1.2.246.562.10.14893989377",
                jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                arviointipaiva = dateFormatter.parse("2024-11-14"),
                tekstinYmmartaminen = 1,
                kirjoittaminen = 1,
                rakenteetJaSanasto = 1,
                puheenYmmartaminen = 2,
                puhuminen = 3,
                yleisarvosana = 1,
                tarkistusarvioinninSaapumisPvm = null,
                tarkistusarvioinninAsiatunnus = null,
                tarkistusarvioidutOsakokeet = null,
                arvosanaMuuttui = null,
                perustelu = null,
                tarkistusarvioinninKasittelyPvm = null,
            )
        val suoritus2 =
            YkiSuoritusEntity(
                id = null,
                suorittajanOID = "1.2.246.562.24.12345678910",
                hetu = "010180-9026",
                sukupuoli = Sukupuoli.E,
                sukunimi = "Testinen",
                etunimet = "Testi",
                kansalaisuus = "EST",
                katuosoite = "Testikuja 2",
                postinumero = "40100",
                postitoimipaikka = "Testilä",
                email = "testi@testi.fi",
                suoritusId = 123456,
                lastModified = Instant.parse("2024-10-30T13:53:56Z"),
                tutkintopaiva = dateFormatter.parse("2024-09-01"),
                tutkintokieli = Tutkintokieli.ENG,
                tutkintotaso = Tutkintotaso.YT,
                jarjestajanTunnusOid = "1.2.246.562.10.14893989377",
                jarjestajanNimi = "Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",
                arviointipaiva = dateFormatter.parse("2024-11-14"),
                tekstinYmmartaminen = 1,
                kirjoittaminen = 1,
                rakenteetJaSanasto = 1,
                puheenYmmartaminen = 2,
                puhuminen = 3,
                yleisarvosana = 1,
                tarkistusarvioinninSaapumisPvm = null,
                tarkistusarvioinninAsiatunnus = null,
                tarkistusarvioidutOsakokeet = null,
                arvosanaMuuttui = null,
                perustelu = null,
                tarkistusarvioinninKasittelyPvm = null,
            )
        val updatedSuoritus =
            suoritus.copy(
                lastModified = Instant.parse("2024-11-01T13:53:56Z"),
                tekstinYmmartaminen = 5,
                kirjoittaminen = 4,
                rakenteetJaSanasto = 3,
                puheenYmmartaminen = 1,
                puhuminen = 2,
                yleisarvosana = 3,
                tarkistusarvioinninSaapumisPvm = dateFormatter.parse("2024-10-01"),
                tarkistusarvioinninAsiatunnus = "123123",
                tarkistusarvioidutOsakokeet = 2,
                arvosanaMuuttui = true,
                perustelu = "Tarkistusarvioinnin testi",
                tarkistusarvioinninKasittelyPvm = dateFormatter.parse("2024-10-15"),
            )
        ykiSuoritusRepository.saveAll(listOf(suoritus, suoritus2, updatedSuoritus))
        val suoritukset = ykiSuoritusRepository.findAllDistinct().toList()
        assertTrue(suoritukset.map { it.copy(id = null) }.containsAll(listOf(suoritus2, updatedSuoritus)))
        assertFalse(suoritukset.map { it.copy(id = null) }.contains(suoritus))
        assertEquals(2, suoritukset.count())
    }
}
