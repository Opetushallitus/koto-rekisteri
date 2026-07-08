package fi.oph.kitu.i18n

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MissingTranslationsTest {
    @AfterEach
    fun clearTolgee() = TolgeeMessages.set(emptyMap())

    @Test
    fun `palauttaa vain avaimet joita ei ole Tolgeessa oletustekstin kanssa`() {
        UiTextRegistry.record("test.puuttuu", "Oletusteksti A")
        UiTextRegistry.record("test.loytyy", "Oletusteksti B")
        TolgeeMessages.set(mapOf("test.loytyy" to LocalizedString(fi = "Oletusteksti B", sv = "Standardtext B")))

        val missing = missingUiTranslations()

        assertEquals("Oletusteksti A", missing["test.puuttuu"])
        assertFalse(missing.containsKey("test.loytyy"), "Tolgeessa jo oleva avain ei saa näkyä puuttuvissa")
    }
}
