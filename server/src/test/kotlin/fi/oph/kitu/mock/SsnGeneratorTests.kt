package fi.oph.kitu.mock

import fi.oph.kitu.yki.Sukupuoli
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SsnGeneratorTests {
    fun assertSSn(ssn: String) {
        assertNotNull(ssn)
        assertTrue(ssn.isNotEmpty())
        assertTrue(
            ssn.matches(
                "((0[1-9]|[12][0-9]|3[01]))((0[1-9]|1[0-2]))(\\d{2})([-AaBbCcDdEeFfXxYyWwVvUu+])(\\d{3})([0-9A-Ya-y])"
                    .toRegex(),
            ),
        )
    }

    @Test
    fun `generate random gen Z SSN`() {
        val ssn = generateRandomSsn(min = LocalDate.of(2000, 1, 1))
        val separator = ssn[6]

        assertSSn(ssn)

        assertTrue(separator == 'A')
    }

    @Test
    fun `generate random 1900's century SSN`() {
        val ssn = generateRandomSsn(max = LocalDate.of(2000, 1, 1))
        val char = ssn[6]

        assertSSn(ssn)

        assertTrue(char == '-')
    }

    @Test
    fun `generate random temporary SSN`() {
        val ssn = generateRandomSsn(isTemporary = true)
        val char = ssn[7]

        assertSSn(ssn)

        assertTrue(char == '9')
    }

    @Test
    fun `generate random real-like SSN`() {
        val ssn = generateRandomSsn(isTemporary = false)
        val char = ssn[7]

        assertSSn(ssn)

        assertTrue(char != '9')
    }

    @Test
    fun `generate random SSN for female`() {
        val ssn = generateRandomSsn(sex = Sukupuoli.N)
        val int = "${ssn[9]}".toInt()

        assertSSn(ssn)

        assertTrue(int % 2 == 0)
    }

    @Test
    fun `generate random SSN for male`() {
        val ssn = generateRandomSsn(sex = Sukupuoli.M)
        val int = "${ssn[9]}".toInt()

        assertSSn(ssn)

        assertTrue(int % 2 != 0)
    }
}
