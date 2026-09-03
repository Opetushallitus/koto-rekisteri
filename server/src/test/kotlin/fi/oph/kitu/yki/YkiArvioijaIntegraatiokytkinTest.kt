package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.yki.arvioijat.solki.SolkiArvioijaService
import fi.oph.kitu.yki.arvioijat.solki.SolkiArvioijaServiceImpl
import fi.oph.kitu.yki.arvioijat.solki.SolkiArvioijaServiceMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Toteutuksen valinta ei saa riippua komponenttiskannauksen jarjestyksesta: vaara valinta olisi
 * hiljaista datan katoamista (mock nielee lahetykset) tai kaynnistysvirhe.
 */
@SpringBootTest(properties = ["kitu.yki.arvioijarekisteri.integraatio.enabled=false"])
@Import(DBContainerConfiguration::class)
class YkiArvioijaIntegraatiokytkinPoisTest(
    @param:Autowired private val service: SolkiArvioijaService,
) {
    @Test
    fun `kytkin pois valitsee varatoteutuksen`() {
        assertTrue(service is SolkiArvioijaServiceMock, "sai ${service.javaClass.simpleName}")
    }
}

@SpringBootTest(properties = ["kitu.yki.arvioijarekisteri.integraatio.enabled=true"])
@Import(DBContainerConfiguration::class)
class YkiArvioijaIntegraatiokytkinPaallaTest(
    @param:Autowired private val service: SolkiArvioijaService,
) {
    @Test
    fun `kytkin paalla valitsee lahettavan toteutuksen`() {
        assertTrue(service is SolkiArvioijaServiceImpl, "sai ${service.javaClass.simpleName}")
    }
}
