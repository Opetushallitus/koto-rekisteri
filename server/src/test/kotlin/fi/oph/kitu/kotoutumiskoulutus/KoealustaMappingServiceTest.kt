package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.oppijanumero.Oppija
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class KoealustaMappingServiceTest(
    @param:Autowired private val mappingService: KoealustaMappingService
) {
    @Test
    fun `test kutsumanimi combinations`() {
        val error = KielitestiSuoritusError(
            id = 1,
            suorittajanOid = null,
            hetu = "010180-9026",
            nimi = "Ranja Testi Öhman-Testi",
            etunimet = "Ranja Testi",
            sukunimi = "Öhman-Testi",
            kutsumanimi = "",
            schoolOid = null,
            teacherEmail = "",
            virheenLuontiaika = Instant.now(),
            viesti = "",
            virheellinenKentta = null,
            virheellinenArvo = null,
            lisatietoja = null
        )
        val expectedOppija = Oppija(
            "Ranja Testi",
            "010180-9026",
            "Ranja",
            "Öhman-Testi",
        )
        val oppija = mappingService.tryEachEtunimiAsKutsumanimi(error)
        assertEquals(expectedOppija, oppija)
    }
}
