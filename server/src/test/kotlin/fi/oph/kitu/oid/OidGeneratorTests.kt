package fi.oph.kitu.oid

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OidGeneratorTests {
    @Test
    fun `generated random SSN`() {
        val ssn = generateRandomUserOid()

        assertNotNull(ssn)
        assertTrue(ssn.isNotEmpty())
        assertTrue(
            ssn.matches(
                Regex(
                    "^1\\.2\\.246\\.562\\.25\\.(\\d{11}+)+\$",
                ),
            ),
        )
        println("checks pass")
    }
}
