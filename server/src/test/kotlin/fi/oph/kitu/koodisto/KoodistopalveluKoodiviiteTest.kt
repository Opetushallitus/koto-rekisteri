package fi.oph.kitu.koodisto

import fi.oph.kitu.i18n.LocalizedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KoodistopalveluKoodiviiteTest {
    @Test
    fun `toLocalizedString kokoaa kolme kielta yhteen objektiin`() {
        val metadata =
            listOf(
                KoodistopalveluKoodiviiteMetadata("Suomi", KoodistopalveluLanguage.FI),
                KoodistopalveluKoodiviiteMetadata("Finland", KoodistopalveluLanguage.SV),
                KoodistopalveluKoodiviiteMetadata("Finland", KoodistopalveluLanguage.EN),
            )

        assertEquals(
            LocalizedString(fi = "Suomi", sv = "Finland", en = "Finland"),
            metadata.toLocalizedString(),
        )
    }

    @Test
    fun `toLocalizedString jattaa puuttuvat kielet nulleiksi`() {
        val metadata = listOf(KoodistopalveluKoodiviiteMetadata("Suomi", KoodistopalveluLanguage.FI))
        val result = metadata.toLocalizedString()
        assertEquals("Suomi", result.fi)
        assertNull(result.sv)
        assertNull(result.en)
    }

    @Test
    fun `toLocalizedString tyhjasta listasta palauttaa pelkkia nulleja`() {
        assertEquals(LocalizedString(), emptyList<KoodistopalveluKoodiviiteMetadata>().toLocalizedString())
    }

    @Test
    fun `toLocalizedString jattaa viimeisen arvon kielikohtaisesti voimaan`() {
        val metadata =
            listOf(
                KoodistopalveluKoodiviiteMetadata("Ensimmäinen", KoodistopalveluLanguage.FI),
                KoodistopalveluKoodiviiteMetadata("Toinen", KoodistopalveluLanguage.FI),
            )
        assertEquals("Toinen", metadata.toLocalizedString().fi)
    }
}
