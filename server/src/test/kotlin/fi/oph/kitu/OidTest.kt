package fi.oph.kitu

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OidTest {
    private val validOidString = "1.2.246.562.10.1234567890"
    private val nonValidOidString = "definitely.not.a.valid.oid.string"

    @Test
    fun `parsing correctly formatted string as OID succeeds`() {
        assertNotNull(Oid.valueOf(validOidString))
    }

    @Test
    fun `parsing incorrectly formatted string as OID returns NULL`() {
        assertNull(Oid.valueOf(nonValidOidString))
    }

    @Test
    fun `converting OID to string yields a correctly formatted OID string`() {
        assertEquals(validOidString, Oid.valueOf(validOidString).toString())
    }

    @Test
    fun `implicit toString yields a correctly formatted OID string`() {
        val oid = Oid.valueOf(validOidString)
        val string = "$oid"
        assertEquals(validOidString, string)
    }
}
