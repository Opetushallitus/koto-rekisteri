package fi.oph.kitu.html

import kotlinx.html.HTMLTag
import kotlinx.html.unsafe
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/**
 * Oletus-safelist virkailija- tai integraatiolähteistä tulevalle HTML:lle.
 *
 * Säilyttää tekstimuotoilun (otsikot, listat, taulukot, linkit, kuvat) sekä
 * tehtäväpankin tarvitsemat `<audio>`/`<video>`-tagit. Poistaa kaikki XSS-
 * vektorit:
 *
 * - `<script>`, `<iframe>`, `<object>`, `<embed>`, `<style>` yms.
 * - event-handler-attribuutit (`onclick`, `onload`, …)
 * - `javascript:`- ja muut tuntemattomat protokollat URL-attribuuteista
 * - vaaralliset CSS:ää sisältävät `style`-attribuutit
 */
val defaultSafeHtmlSafelist: Safelist =
    Safelist
        .relaxed()
        .addTags("audio", "video", "source", "figure", "figcaption")
        .addAttributes("audio", "controls", "src", "preload")
        .addAttributes("video", "controls", "src", "poster", "preload")
        .addAttributes("source", "src", "type")
        .addProtocols("audio", "src", "http", "https")
        .addProtocols("video", "src", "http", "https")
        .addProtocols("source", "src", "http", "https")

/**
 * Renderöi annetun HTML-merkkijonon turvallisesti kotlinx.html-puuhun: ajaa
 * sen Jsoupin sanitaattorin läpi, joka poistaa `<script>`-tagit, event-
 * handlerit, `javascript:`-URL:t ym. tunnetut XSS-vektorit. Käytä aina kun
 * lähde on käyttäjän tai integraation tuottamaa HTML:ää eikä sitä haluta
 * päästää suoraan DOM:iin.
 *
 * Kutsutaan minkä tahansa kontti-elementin sisältä, esim.
 * `div("teksti") { safeHtml(rawHtml) }`.
 */
fun HTMLTag.safeHtml(
    html: String,
    safelist: Safelist = defaultSafeHtmlSafelist,
) {
    unsafe { +Jsoup.clean(html, safelist) }
}
