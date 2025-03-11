package fi.oph.kitu

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OidTest {
    private val validOidString = "1.2.246.562.10.1234567890"
    private val nonValidOidString = "definitely.not.a.valid.oid.string"

    @Test
    fun `parsing correctly formatted string as OID succeeds`() {
        assertTrue(Oid.parse(validOidString).isSuccess)
    }

    @Test
    fun `parsing incorrectly formatted string as OID returns a failure`() {
        assertTrue(Oid.parse(nonValidOidString).isFailure)
    }

    @Test
    fun `converting OID to string yields a correctly formatted OID string`() {
        assertEquals(validOidString, Oid.parse(validOidString).getOrThrow().toString())
    }

    @Test
    fun `implicit toString yields a correctly formatted OID string`() {
        val oid = Oid.parse(validOidString).getOrThrow()
        val string = "$oid"
        assertEquals(validOidString, string)
    }
}
