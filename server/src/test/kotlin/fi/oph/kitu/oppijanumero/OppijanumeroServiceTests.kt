package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.assertFailureIsThrowable
import fi.oph.kitu.logging.MockTracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import kotlin.test.assertEquals

class OppijanumeroServiceTests {
    fun addCasAuthenticatedService(
        mockServer: MockRestServiceServer,
        serviceBaseUrl: String,
        expectedUri: String,
        respondWithJsonBody: String,
    ): RestClient.Builder {
        val casBaseUrl = "http://localhost:8080/cas"

        val builder = RestClient.builder().baseUrl(casBaseUrl)
        val casMockServer =
            MockRestServiceServer
                .bindTo(builder)
                .ignoreExpectOrder(true)
                .build()

        // Expectation 1: App tries to use CAS-authenticated site.
        // It does not have the correct cookie, so CAS returns HTTP 302
        mockServer
            .expect(requestTo(expectedUri))
            .andExpect { request ->
                val cookieHeader = request.headers.getFirst("Cookie")
                if (cookieHeader != null && cookieHeader.contains("JSESSIONID=")) {
                    throw AssertionError("Unexpected JSESSIONID cookie present")
                }
            }.andRespond(
                withStatus(HttpStatus.FOUND)
                    .location(URI("http://localhost:8080/cas/v1/cas/login")),
            )

        // Expectation 2: getGrantingTicket - Since CAS provided login - site, now we have to get granting ticket:
        casMockServer
            .expect(requestTo("http://localhost:8080/cas/v1/tickets"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string("username=username&password=password"))
            .andRespond(
                withStatus(HttpStatus.CREATED)
                    .header("Location", "http://localhost:8080/v1/tickets/12345-6"),
            )

        // Expectation 3: getServiceTicket - After granting ticket, -> service ticket
        // No further requests expected: HTTP POST http://localhost:8080/cas/v1/tickets/12345-6
        casMockServer
            .expect(requestTo("http://localhost:8080/cas/v1/tickets/12345-6"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andExpect(content().string("service=http://localhost:8080/cas/login/j_spring_cas_security_check"))
            .andRespond(
                withSuccess(
                    "65432-1",
                    MediaType.TEXT_HTML,
                ),
            )

        // Exepectation 4: verifyServiceTicket - service ticket validated in CAS Authenticated service
        mockServer
            .expect(requestTo("$serviceBaseUrl/j_spring_cas_security_check?ticket=65432-1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.FOUND)
                    .header("Location", expectedUri),
            )

        // Expectation 5: Authenticated request
        mockServer
            .expect(requestTo(expectedUri))
            .andRespond(withSuccess(respondWithJsonBody, MediaType.APPLICATION_JSON))
        return builder
    }

    @Test
    fun `oppijanumero service returns identified user`() {
        // Facade
        val expectedOppijanumero = Oid.parse("1.2.246.562.24.33342764709").getOrThrow()
        val onrRestClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumero-service")
        val mockServer =
            MockRestServiceServer
                .bindTo(onrRestClientBuilder)
                .ignoreExpectOrder(true)
                .build()

        val casRestClientBuilder =
            addCasAuthenticatedService(
                mockServer,
                serviceBaseUrl = "http://localhost:8080/oppijanumero-service",
                "http://localhost:8080/oppijanumero-service/yleistunniste/hae",
                """
                {
                    "oid": "1.2.246.562.24.33342764709",
                    "oppijanumero": "1.2.246.562.24.33342764709"
                }
                """.trimIndent(),
            )

        val restClient = onrRestClientBuilder.build()
        val casRestClient = casRestClientBuilder.build()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                casAuthenticatedService =
                    CasAuthenticatedService(
                        restClient = restClient,
                        casService =
                            CasService(
                                casRestClient,
                                restClient,
                            ).apply {
                                serviceUrl = "http://localhost:8080/cas/login"
                                onrUsername = "username"
                                onrPassword = "password"
                            },
                    ),
                objectMapper = ObjectMapper(),
                tracer = MockTracer(),
            )

        val result =
            oppijanumeroService
                .getOppijanumero(
                    Oppija(
                        "Magdalena Testi",
                        "Sallinen-Testi",
                        "Magdalena",
                        "010866-9260",
                    ),
                ).getOrThrow()
        assertEquals(expectedOppijanumero, result)
    }

    @Test
    fun `oppijanumero service returns unidentified user`() {
        // Facade
        val restClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumero-service")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
            .andRespond(
                withSuccess(
                    """
                    {
                        "oid": "1.2.246.562.24.33342764709",
                        "oppijanumero": ""
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val restClient = restClientBuilder.build()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                casAuthenticatedService =
                    CasAuthenticatedService(
                        restClient = restClient,
                        casService =
                            CasService(
                                restClient,
                                restClient,
                            ),
                    ),
                objectMapper = ObjectMapper(),
                tracer = MockTracer(),
            )

        // System under test
        assertThrows<OppijanumeroException.OppijaNotIdentifiedException> {
            oppijanumeroService
                .getOppijanumero(
                    Oppija(
                        "Magdalena Testi",
                        "Sallinen-Testi",
                        "Magdalena",
                        "010866-9260",
                    ),
                ).getOrThrow()
        }
    }

    @Test
    fun `oppijanumero service does not find user`() {
        // Facade
        val restClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumero-service")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """
                        {
                            "timestamp": 1734962667439,
                            "status":404,
                            "error":"Not Found",
                            "path":"/oppijanumerorekisteri-service/yleistunniste/hae"
                        }
                        """.trimIndent(),
                    ),
            )

        val restClient = restClientBuilder.build()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                casAuthenticatedService =
                    CasAuthenticatedService(
                        restClient = restClient,
                        casService =
                            CasService(
                                restClient,
                                restClient,
                            ),
                    ),
                objectMapper = ObjectMapper(),
                tracer = MockTracer(),
            )

        assertThrows<OppijanumeroException.OppijaNotFoundException> {
            oppijanumeroService
                .getOppijanumero(
                    Oppija(
                        "Magdalena Testi",
                        "Sallinen-Testi",
                        "Magdalena",
                        "010866-9260",
                    ),
                ).getOrThrow()
        }
    }

    @Test
    fun `oppijanumero service received bad request`() {
        // Facade
        val restClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumero-service")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """
                        {
                            "timestamp": 1734962667439,
                            "status":409,
                            "error":"Conflict",
                            "path":"/oppijanumerorekisteri-service/yleistunniste/hae"
                        }
                        """.trimIndent(),
                    ),
            )

        val restClient = restClientBuilder.build()
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                casAuthenticatedService =
                    CasAuthenticatedService(
                        restClient = restClient,
                        casService =
                            CasService(
                                restClient,
                                restClient,
                            ),
                    ),
                objectMapper = ObjectMapper(),
                tracer = MockTracer(),
            )

        val result =
            oppijanumeroService.getOppijanumero(
                Oppija(
                    "Magdalena Testi",
                    "Sallinen-Testi",
                    "Magdalena",
                    "010866-9260",
                ),
            )

        assertFailureIsThrowable<OppijanumeroException.BadRequest>(
            result,
            "Bad request to oppijanumero-service",
        )
    }
}
