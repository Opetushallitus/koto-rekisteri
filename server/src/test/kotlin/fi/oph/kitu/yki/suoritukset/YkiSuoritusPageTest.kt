package fi.oph.kitu.yki.suoritukset

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YkiSuoritusPageTest {
    private fun renderNimitieto(
        value: String,
        onrValue: String?,
    ) = with(YkiSuoritusPage) {
        createHTML().div { nimitieto(value, onrValue) }
    }

    @Test
    fun `nimitieto renders only the suoritus value when there is no ONR value`() {
        val html = renderNimitieto("Meikäläinen", null)

        assertTrue(html.contains("<span>Meikäläinen</span>"), "missing suoritus value:\n$html")
        assertFalse(html.contains("warning-pill"), "should not warn without an ONR value:\n$html")
    }

    @Test
    fun `nimitieto does not warn when the ONR value matches`() {
        val html = renderNimitieto("Meikäläinen", "Meikäläinen")

        assertTrue(html.contains("<span>Meikäläinen</span>"), "missing suoritus value:\n$html")
        assertFalse(html.contains("warning-pill"), "should not warn when values match:\n$html")
    }

    @Test
    fun `nimitieto warns and shows the ONR value when it differs`() {
        val html = renderNimitieto("Meikäläinen", "Möykäläinen")

        assertTrue(html.contains("<span>Meikäläinen</span>"), "missing suoritus value:\n$html")
        assertTrue(html.contains("""class="warning-pill""""), "missing warning pill:\n$html")
        assertTrue(
            html.contains("Eri arvo oppijanumerorekisterissä:"),
            "missing warning text:\n$html",
        )
        assertTrue(html.contains("<strong>Möykäläinen</strong>"), "missing ONR value:\n$html")
    }
}
