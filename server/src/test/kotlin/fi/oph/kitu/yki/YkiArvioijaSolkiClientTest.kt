package fi.oph.kitu.yki

import fi.oph.kitu.restclient.withLenientStringConverter
import fi.oph.kitu.yki.arvioijat.solki.SolkiArvioijaClientImpl
import fi.oph.kitu.yki.arvioijat.solki.SolkiArvioijaException
import fi.oph.kitu.yki.arvioijat.solki.SolkiArvioijaRequest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Vastauskohtainen kasittely: yhteysvirhe ei saa vuotaa poikkeuksena kutsujalle. */
class YkiArvioijaSolkiClientTest {
    private val versio = OffsetDateTime.of(2026, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC)

    private fun clientJaServer(): Pair<SolkiArvioijaClientImpl, MockRestServiceServer> {
        val builder = RestClient.builder().baseUrl("https://solki.test/oph/").withLenientStringConverter()
        val server = MockRestServiceServer.bindTo(builder).build()
        return SolkiArvioijaClientImpl(builder.build()) to server
    }

    private fun request() =
        SolkiArvioijaRequest(
            arvioijanOppijanumero = "1.2.246.562.24.20281155246",
            versio = versio,
            sukunimi = "Öhman-Testi",
            etunimet = "Ranja Testi",
            sahkopostiosoite = "testi@testi.fi",
            katuosoite = "Testikuja 5",
            postinumero = "40100",
            postitoimipaikka = "Testila",
            arviointioikeudet = emptyList(),
        )

    @Test
    fun `yhteysvirhe palautuu Leftina eika poikkeuksena`() {
        val (client, server) = clientJaServer()
        server
            .expect(requestTo("https://solki.test/oph/arvioijat/1.2.246.562.24.20281155246"))
            .andRespond(withException(IOException("connection refused")))

        val tulos = client.put(request())

        assertTrue(
            tulos.leftOrNull() is SolkiArvioijaException.ConnectionFailure,
            "yhteysvirheen on tultava Leftina, muuten tallennuspolku kaatuu: $tulos",
        )
    }

    @Test
    fun `konflikti tulkitaan onnistumiseksi`() {
        val (client, server) = clientJaServer()
        server.expect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.CONFLICT))

        assertTrue(client.put(request()).isRight(), "Solkilla on uudempi versio, ei virhe")
    }

    @Test
    fun `palvelinvirhe on UnexpectedError ja idempotency-key on mukana`() {
        val (client, server) = clientJaServer()
        server
            .expect(header("Idempotency-Key", "1.2.246.562.24.20281155246:$versio"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("hajosi"))

        val tulos = client.put(request())

        assertTrue(tulos.leftOrNull() is SolkiArvioijaException.UnexpectedError)
    }

    @Test
    fun `onnistunut lahetys palauttaa Rightin`() {
        val (client, server) = clientJaServer()
        server.expect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.NO_CONTENT))

        assertEquals(true, client.put(request()).isRight())
    }
}
