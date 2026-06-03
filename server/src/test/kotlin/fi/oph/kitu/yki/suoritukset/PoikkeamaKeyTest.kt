package fi.oph.kitu.yki.suoritukset

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PoikkeamaKeyTest {
    @Test
    fun `encode tuottaa solkiId-kentta -formaatin`() {
        assertEquals("12345:tutkintopaiva", PoikkeamaKey(12345, "tutkintopaiva").encode())
    }

    @Test
    fun `decode purkaa solkiId-kentta -formaatin`() {
        assertEquals(PoikkeamaKey(12345, "tutkintopaiva"), PoikkeamaKey.decode("12345:tutkintopaiva"))
    }

    @Test
    fun `decode palauttaa nullin jos solkiId puuttuu`() {
        assertNull(PoikkeamaKey.decode(":tutkintopaiva"))
    }

    @Test
    fun `decode palauttaa nullin jos kentta puuttuu`() {
        assertNull(PoikkeamaKey.decode("12345:"))
        assertNull(PoikkeamaKey.decode("12345"))
    }

    @Test
    fun `decode palauttaa nullin jos solkiId ei ole numero`() {
        assertNull(PoikkeamaKey.decode("abc:tutkintopaiva"))
    }

    @Test
    fun `decode säilyttää kentassa olevat kaksoispisteet`() {
        assertEquals(PoikkeamaKey(1, "foo:bar"), PoikkeamaKey.decode("1:foo:bar"))
    }

    @Test
    fun `encode ja decode round-trip`() {
        val original = PoikkeamaKey(987654, "tarkistusarvioinninAsiatunnus")
        assertEquals(original, PoikkeamaKey.decode(original.encode()))
    }
}
