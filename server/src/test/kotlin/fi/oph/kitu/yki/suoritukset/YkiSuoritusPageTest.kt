package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.dev.mockdata.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.TutkinnonOsa
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

    private fun renderArviointi(suoritus: YkiSuoritusEntity) =
        with(YkiSuoritusPage) {
            createHTML().div { arviointi(suoritus) }
        }

    private fun ilmoittautunut(vararg osakokeet: TutkinnonOsa) =
        generateRandomYkiSuoritusEntity()
            .copy(
                arviointitila = Arviointitila.ILMOITTAUTUNUT,
                arviointipaiva = null,
                tekstinYmmartaminen = null,
                kirjoittaminen = null,
                rakenteetJaSanasto = null,
                puheenYmmartaminen = null,
                puhuminen = null,
                yleisarvosana = null,
                tarkistusarvioinninSaapumisPvm = null,
                tarkistusarvioinninAsiatunnus = null,
                tarkistusarvioidutOsakokeet = null,
                arvosanaMuuttui = null,
                perustelu = null,
                tarkistusarvioinninKasittelyPvm = null,
            ).also { it.ilmoitetutOsakokeet = osakokeet.toSet() }

    @Test
    fun `arviointi renders registered osakokeet even without arvosana`() {
        val html = renderArviointi(ilmoittautunut(TutkinnonOsa.TY, TutkinnonOsa.KI))

        assertTrue(html.contains(TutkinnonOsa.TY.viewText), "missing tekstin ymmärtäminen row:\n$html")
        assertTrue(html.contains(TutkinnonOsa.KI.viewText), "missing kirjoittaminen row:\n$html")
    }

    @Test
    fun `arviointi does not render osakokeet that were not registered`() {
        val html = renderArviointi(ilmoittautunut(TutkinnonOsa.TY))

        assertTrue(html.contains(TutkinnonOsa.TY.viewText), "missing tekstin ymmärtäminen row:\n$html")
        assertFalse(html.contains(TutkinnonOsa.YL.viewText), "unregistered yleisarvosana should not render:\n$html")
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
