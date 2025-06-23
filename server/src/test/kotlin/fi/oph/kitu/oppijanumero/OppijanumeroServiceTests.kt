package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.assertFailureIsThrowable
import fi.oph.kitu.logging.MockTracer
import org.junit.jupiter.api.Test
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
        val expectedOppijanumero = Oid.parse("1.2.246.562.24.33342764709").getOrThrow()
        val onrRestClientBuilder = RestClient.builder().baseUrl("http://localhost:8080/oppijanumero-service")
        val mockServer =
            MockRestServiceServer
                .bindTo(onrRestClientBuilder)
                .ignoreExpectOrder(true)
                .build()

        val casRestClientBuilder = createRestClientBuilderWithCasFlow("http://localhost:8080/cas")

        mockServer
            .addCasFlow(
                serviceBaseUrl = "http://localhost:8080/oppijanumero-service",
                serviceEndpoint = "yleistunniste/hae",
            ).expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
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

        val oppijanumeroRestClient = onrRestClientBuilder.build()
        val casRestClient = casRestClientBuilder.build()

        val objectMapper = ObjectMapper()
        val tracer = MockTracer()
        val oppijanumeroService =
            OppijanumeroService(
                tracer,
                OppijanumerorekisteriClient(
                    CasAuthenticatedServiceImpl(
                        oppijanumeroRestClient,
                        CasService(
                            casRestClient,
                            oppijanumeroRestClient,
                        ).apply {
                            serviceUrl = "http://localhost:8080/cas/login"
                            onrUsername = "username"
                            onrPassword = "password"
                        },
                        tracer,
                    ),
                    objectMapper,
                ).apply {
                    serviceUrl = "http://localhost:8080/oppijanumero-service"
                },
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
        val casRestClientBuilder = createRestClientBuilderWithCasFlow("http://localhost:8080/cas")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()

        mockServer
            .addCasFlow(
                serviceBaseUrl = "http://localhost:8080/oppijanumero-service",
                serviceEndpoint = "yleistunniste/hae",
            ).expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
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
        val casRestClient = casRestClientBuilder.build()
        val oppijanumeroRestClient = restClientBuilder.build()
        val objectMapper = ObjectMapper()
        val tracer = MockTracer()
        val oppijanumeroService =
            OppijanumeroService(
                tracer,
                OppijanumerorekisteriClient(
                    CasAuthenticatedServiceImpl(
                        oppijanumeroRestClient,
                        CasService(
                            casRestClient,
                            oppijanumeroRestClient,
                        ).apply {
                            serviceUrl = "http://localhost:8080/cas/login"
                            onrUsername = "username"
                            onrPassword = "password"
                        },
                        tracer,
                    ),
                    objectMapper,
                ).apply {
                    serviceUrl = "http://localhost:8080/oppijanumero-service"
                },
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
        val casRestClientBuilder = createRestClientBuilderWithCasFlow("http://localhost:8080/cas")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .addCasFlow(
                serviceBaseUrl = "http://localhost:8080/oppijanumero-service",
                serviceEndpoint = "yleistunniste/hae",
            ).expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
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
        val casRestClient = casRestClientBuilder.build()
        val oppijanumeroRestClient = restClientBuilder.build()
        val objectMapper = ObjectMapper()
        val tracer = MockTracer()
        val oppijanumeroService =
            OppijanumeroService(
                tracer,
                OppijanumerorekisteriClient(
                    CasAuthenticatedServiceImpl(
                        oppijanumeroRestClient,
                        CasService(
                            casRestClient,
                            oppijanumeroRestClient,
                        ).apply {
                            serviceUrl = "http://localhost:8080/cas/login"
                            onrUsername = "username"
                            onrPassword = "password"
                        },
                        tracer,
                    ),
                    objectMapper,
                ).apply {
                    serviceUrl = "http://localhost:8080/oppijanumero-service"
                },
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
        val casRestClientBuilder = createRestClientBuilderWithCasFlow("http://localhost:8080/cas")
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        mockServer
            .addCasFlow(
                serviceBaseUrl = "http://localhost:8080/oppijanumero-service",
                serviceEndpoint = "yleistunniste/hae",
            ).expect(requestTo("http://localhost:8080/oppijanumero-service/yleistunniste/hae"))
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
        val casRestClient = casRestClientBuilder.build()
        val oppijanumeroRestClient = restClientBuilder.build()
        val objectMapper = ObjectMapper()
        val tracer = MockTracer()
        val oppijanumeroService =
            OppijanumeroService(
                tracer,
                OppijanumerorekisteriClient(
                    CasAuthenticatedServiceImpl(
                        oppijanumeroRestClient,
                        CasService(
                            casRestClient,
                            oppijanumeroRestClient,
                        ).apply {
                            serviceUrl = "http://localhost:8080/cas/login"
                            onrUsername = "username"
                            onrPassword = "password"
                        },
                        tracer,
                    ),
                    objectMapper,
                ).apply {
                    serviceUrl = "http://localhost:8080/oppijanumero-service"
                },
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
