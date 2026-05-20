package fi.oph.kitu.organisaatiot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KoodiviiteUriTest {
    @Test
    fun `koodistoUri ja koodiarvo erotetaan alaviivasta`() {
        val uri = KoodiviiteUri("kieli_FI")
        assertEquals("kieli", uri.koodistoUri)
        assertEquals("FI", uri.koodiarvo)
        assertNull(uri.versio)
    }

    @Test
    fun `numeerinen koodiarvo`() {
        val uri = KoodiviiteUri("organisaatiotyyppi_02")
        assertEquals("organisaatiotyyppi", uri.koodistoUri)
        assertEquals("02", uri.koodiarvo)
    }

    @Test
    fun `versio luetaan ristikon jalkeen`() {
        val uri = KoodiviiteUri("organisaatiotyyppi_02#1")
        assertEquals("organisaatiotyyppi", uri.koodistoUri)
        assertEquals("02", uri.koodiarvo)
        assertEquals(1, uri.versio)
    }

    @Test
    fun `puuttuva versio palauttaa nullin`() {
        val uri = KoodiviiteUri("kieli_FI")
        assertNull(uri.versio)
    }

    @Test
    fun `viite sailyy data classin tasa-arvossa`() {
        assertEquals(KoodiviiteUri("kieli_FI"), KoodiviiteUri("kieli_FI"))
    }
}
