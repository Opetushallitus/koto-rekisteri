package fi.oph.kitu.html

import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeHtmlTest {
    private fun render(html: String): String =
        createHTML().html {
            body {
                div { safeHtml(html) }
            }
        }

    @Test
    fun `script-tagit poistetaan`() {
        val out = render("<p>ok</p><script>alert('xss')</script>")
        assertFalse("<script" in out, "Tulokseen jäi script-tagi: $out")
        assertFalse("alert" in out, "Tulokseen jäi script-sisältö: $out")
        assertTrue("<p>ok</p>" in out)
    }

    @Test
    fun `inline-event-handlerit poistetaan`() {
        val out = render("""<img src="x.png" onerror="alert(1)" alt="kuva">""")
        assertFalse("onerror" in out)
        assertTrue("<img" in out)
    }

    @Test
    fun `javascript-protokolla URL-attribuutista poistetaan`() {
        val out = render("""<a href="javascript:alert(1)">click</a>""")
        assertFalse("javascript:" in out, "javascript:-URL jäi tulokseen: $out")
    }

    @Test
    fun `iframe poistetaan`() {
        val out = render("""<iframe src="https://evil.example/"></iframe>""")
        assertFalse("<iframe" in out)
    }

    @Test
    fun `audio-tagi sallitaan`() {
        val out = render("""<audio controls src="https://example.com/a.mp3"></audio>""")
        assertTrue("<audio" in out)
        assertTrue("controls" in out)
    }

    @Test
    fun `style-tag poistetaan kokonaan`() {
        val out = render("<style>body{display:none}</style><p>ok</p>")
        assertFalse("<style" in out)
        assertFalse("display:none" in out)
        assertTrue("<p>ok</p>" in out)
    }

    @Test
    fun `tavallinen tekstimuotoilu sailyy`() {
        val out = render("<p>Hei <strong>maailma</strong>!</p>")
        assertTrue("<p>Hei <strong>maailma</strong>!</p>" in out)
    }

    @Test
    fun `tyhja syote tuottaa tyhjan elementin`() {
        val out = render("")
        assertTrue("<div></div>" in out.replace("\n", "").replace("  ", ""))
    }
}
