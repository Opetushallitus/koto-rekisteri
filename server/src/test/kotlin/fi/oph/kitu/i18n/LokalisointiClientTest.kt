package fi.oph.kitu.i18n

import io.opentelemetry.api.OpenTelemetry
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LokalisointiClientTest {
    private val tracer = OpenTelemetry.noop().getTracer("test")
    private val baseUrl = "https://virkailija.opintopolku.fi"

    @Test
    fun `yhdistaa kielikohtaiset kaannokset LocalizedStringeiksi`() {
        val builder = RestClient.builder().baseUrl(baseUrl)
        val server = MockRestServiceServer.bindTo(builder).build()

        server
            .expect(requestTo("$baseUrl/lokalisointi/tolgee/kielitutkintorekisteri/fi.json"))
            .andRespond(withSuccess("""{"nav.yki":"Yleinen kielitutkinto"}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("$baseUrl/lokalisointi/tolgee/kielitutkintorekisteri/sv.json"))
            .andRespond(withSuccess("""{"nav.yki":"Allmän språkexamen"}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("$baseUrl/lokalisointi/tolgee/kielitutkintorekisteri/en.json"))
            .andRespond(withSuccess("""{"nav.arvioijat":"Assessors"}""", MediaType.APPLICATION_JSON))

        val client = LokalisointiClient(builder.build(), tracer, "kielitutkintorekisteri")

        val result = client.fetchAll()

        assertEquals("Yleinen kielitutkinto", result["nav.yki"]?.fi)
        assertEquals("Allmän språkexamen", result["nav.yki"]?.sv)
        assertNull(result["nav.yki"]?.en)
        assertEquals("Assessors", result["nav.arvioijat"]?.en)
        assertNull(result["nav.arvioijat"]?.fi)
        server.verify()
    }
}
