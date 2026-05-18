package fi.oph.kitu.html

import kotlinx.html.section
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewMessageTest {
    @Test
    fun `plain-text ViewMessageData escapes HTML markup`() {
        val payload = """<img src=x onerror="alert(1)">"""
        val data = ViewMessageData(payload, ViewMessageType.ERROR)

        val html = createHTML().section { viewMessage(data) }

        assertFalse(
            html.contains("<img"),
            "Plain-string ViewMessage must not render raw HTML — got:\n$html",
        )
        assertTrue(
            html.contains("&lt;img"),
            "Expected the markup to be HTML-escaped — got:\n$html",
        )
    }

    @Test
    fun `html ViewMessageData renders the DSL output verbatim`() {
        val data =
            ViewMessageData.html(ViewMessageType.SUCCESS) {
                section { +"Tallennettu" }
            }

        val html = createHTML().section { viewMessage(data) }

        assertTrue(
            html.contains("<section>Tallennettu</section>"),
            "html() builder output should pass through unescaped — got:\n$html",
        )
    }
}
