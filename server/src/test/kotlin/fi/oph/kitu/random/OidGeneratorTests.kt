package fi.oph.kitu.random

import fi.oph.kitu.random.generateRandomUserOid
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OidGeneratorTests {
    @Test
    fun `generated random user OID`() {
        val oid = generateRandomUserOid().toString()

        assertNotNull(oid)
        assertTrue(oid.isNotEmpty())
        assertTrue(
            oid.matches(
                """^1\.2\.246\.562\.240\.\d{11}$""".toRegex(),
            ),
        )
    }

    @Test
    fun `generated random organization OID`() {
        val oid = generateRandomOrganizationOid().toString()

        assertNotNull(oid)
        assertTrue(oid.isNotEmpty())
        assertTrue(
            oid.matches(
                """^1\.2\.246\.562\.100\.\d{11}$""".toRegex(),
            ),
        )
    }
}
