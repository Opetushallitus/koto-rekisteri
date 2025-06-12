package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.assertFailureIsThrowable
import fi.oph.kitu.logging.MockTracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

class OppijanumeroServiceTests {
    @Test
    fun `oppijanumero service returns identified user`() {
        // Facade
        val restClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumero-service")
        val expectedOppijanumero = Oid.parse("1.2.246.562.24.33342764709").getOrThrow()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
            .andRespond(
                withSuccess(
                    """
                    {
                        "oid": "1.2.246.562.24.33342764709",
                        "oppijanumero": "1.2.246.562.24.33342764709"
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

        val result =
            assertDoesNotThrow {
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
