package fi.oph.kitu.webmvc

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class MissingTranslationsWarningTest {
    private fun render(count: Int): String = createHTML().div { missingTranslationsWarning(count) }

    @Test
    fun `nayttaa lukumaaran ja latauslinkin kun kaannoksia puuttuu`() {
        val html = render(3)

        assertContains(html, "Tolgeesta puuttuu 3 käännösavainta")
        assertContains(html, "warning-text")
        assertContains(html, """href="/kielitutkinnot/lokalisointi/puuttuvat-kaannokset"""")
        assertContains(html, """download="puuttuvat-kaannokset.json"""")
    }

    @Test
    fun `ei renderoi varoitusta kun kaannoksia ei puutu`() {
        assertFalse(render(0).contains("warning-text"), "Nollamäärällä ei näytetä varoitusta")
    }
}
