package fi.oph.kitu.koodisto

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.springframework.http.HttpMethod as SpringHttpMethod

class KoodistoServiceTest {
    private val baseUrl = "http://koodistopalvelu.test"
    private val tracer: Tracer = OpenTelemetry.noop().getTracer("KoodistoServiceTest")

    private lateinit var builder: RestClient.Builder
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var service: KoodistoServiceImpl

    @BeforeTest
    fun setup() {
        builder = RestClient.builder().baseUrl(baseUrl)
        mockServer = MockRestServiceServer.bindTo(builder).build()
        service = KoodistoServiceImpl(builder.build(), tracer)
    }

    @Test
    fun `getKoodiviitteet hakee koodistopalvelusta ja palauttaa parsitut viitteet`() {
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/maatjavaltiot1"))
            .andExpect(method(SpringHttpMethod.GET))
            .andRespond(
                withSuccess(MAAT_JA_VALTIOT_PAYLOAD, MediaType.APPLICATION_JSON),
            )

        val viitteet = service.getKoodiviitteet("maatjavaltiot1")

        assertNotNull(viitteet)
        assertEquals(listOf("FIN", "EST"), viitteet.map { it.koodiArvo })
        assertEquals(2, viitteet.first().versio)
        assertEquals(
            "Suomi",
            viitteet
                .first()
                .metadata
                .first()
                .nimi,
        )
        mockServer.verify()
    }

    @Test
    fun `tulokset tallennetaan valimuistiin eika palvelua kutsuta uudelleen`() {
        mockServer
            .expect(ExpectedCount.once(), requestTo("$baseUrl/codeelement/codes/maatjavaltiot1"))
            .andRespond(withSuccess(MAAT_JA_VALTIOT_PAYLOAD, MediaType.APPLICATION_JSON))

        val first = service.getKoodiviitteet("maatjavaltiot1")
        val second = service.getKoodiviitteet("maatjavaltiot1")

        assertEquals(first, second)
        mockServer.verify()
    }

    @Test
    fun `eri koodisto-URIt kutsuvat palvelua erikseen`() {
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/maatjavaltiot1"))
            .andRespond(withSuccess(MAAT_JA_VALTIOT_PAYLOAD, MediaType.APPLICATION_JSON))
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/kunta"))
            .andRespond(withSuccess(KUNTA_PAYLOAD, MediaType.APPLICATION_JSON))

        val maat = service.getKoodiviitteet("maatjavaltiot1")
        val kunnat = service.getKoodiviitteet("kunta")

        assertEquals(listOf("FIN", "EST"), maat?.map { it.koodiArvo })
        assertEquals(listOf("091"), kunnat?.map { it.koodiArvo })
        mockServer.verify()
    }

    @Test
    fun `tyhja vastaus palauttaa tyhjan listan ja tallentaa sen valimuistiin`() {
        mockServer
            .expect(ExpectedCount.once(), requestTo("$baseUrl/codeelement/codes/tyhja"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

        assertEquals(emptyList(), service.getKoodiviitteet("tyhja"))
        // Toinen kutsu samalla avaimella ei mene palveluun, koska tyhja lista tallennetaan.
        assertEquals(emptyList(), service.getKoodiviitteet("tyhja"))
        mockServer.verify()
    }

    @Test
    fun `204 No Content palauttaa nullin ja seuraava kutsu menee uudelleen palveluun`() {
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/puuttuu"))
            .andRespond(withStatus(HttpStatus.NO_CONTENT))
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/puuttuu"))
            .andRespond(withSuccess(KUNTA_PAYLOAD, MediaType.APPLICATION_JSON))

        assertNull(service.getKoodiviitteet("puuttuu"))

        // null-tulosta ei tallenneta valimuistiin -> seuraava kutsu menee uudelleen palveluun.
        val toinen = service.getKoodiviitteet("puuttuu")
        assertEquals(listOf("091"), toinen?.map { it.koodiArvo })
        mockServer.verify()
    }

    @Test
    fun `404 levia ei nielaista vaan virhe nousee soittajaan asti`() {
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/ei-ole"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertFailsWith<HttpClientErrorException.NotFound> {
            service.getKoodiviitteet("ei-ole")
        }
        mockServer.verify()
    }

    @Test
    fun `palvelinvirheella poikkeus levia ja epaonnistunutta vastausta ei tallenneta valimuistiin`() {
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/rikki"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))
        mockServer
            .expect(requestTo("$baseUrl/codeelement/codes/rikki"))
            .andRespond(withSuccess(KUNTA_PAYLOAD, MediaType.APPLICATION_JSON))

        assertFailsWith<HttpServerErrorException.InternalServerError> {
            service.getKoodiviitteet("rikki")
        }

        // Seuraavan kutsun pitaa silti kayttaa palvelua.
        assertEquals(listOf("091"), service.getKoodiviitteet("rikki")?.map { it.koodiArvo })
        mockServer.verify()
    }

    companion object {
        private val MAAT_JA_VALTIOT_PAYLOAD =
            """
            [
              {
                "koodiUri": "maatjavaltiot1_fin",
                "koodiArvo": "FIN",
                "versio": 2,
                "metadata": [
                  {"nimi": "Suomi", "kieli": "FI"},
                  {"nimi": "Finland", "kieli": "SV"},
                  {"nimi": "Finland", "kieli": "EN"}
                ]
              },
              {
                "koodiUri": "maatjavaltiot1_est",
                "koodiArvo": "EST",
                "versio": 2,
                "metadata": [
                  {"nimi": "Viro", "kieli": "FI"}
                ]
              }
            ]
            """.trimIndent()

        private val KUNTA_PAYLOAD =
            """
            [
              {
                "koodiUri": "kunta_091",
                "koodiArvo": "091",
                "versio": 2,
                "metadata": [
                  {"nimi": "Helsinki", "kieli": "FI"}
                ]
              }
            ]
            """.trimIndent()
    }
}
