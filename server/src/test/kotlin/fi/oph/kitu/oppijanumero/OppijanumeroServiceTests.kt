package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import kotlin.test.assertEquals

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OppijanumeroServiceTests {
    @TestConfiguration
    class RestClientBuilderConfig {
        @Bean
        fun restClientBuilder() = RestClient.builder()
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")!!
    }

    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var oppijanumeroUrl: String

    fun MockRestServiceServer.yleisTunniste(url: String): MockRestServiceServer {
        this
            .expect(requestTo("$oppijanumeroUrl/$url"))
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

        return this
    }

    @Test
    fun `oppijanumero service returns identified user`(
        @Autowired oppijanumeroService: OppijanumeroService,
        @Autowired restClientBuilder: RestClient.Builder,
    ) {
        // Facade
        val expectedOppijanumero = Oid.parse("1.2.246.562.24.33342764709").getOrThrow()
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()

        server
            .yleisTunniste("hae")
            .yleisTunniste("/hae")
            .yleisTunniste("yleistunniste/hae")
            .yleisTunniste("/yleistunniste/hae")
            .yleisTunniste("oppijanumero-service/yleistunniste/hae")
            .yleisTunniste("/oppijanumero-service/yleistunniste/hae")

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

    /*
        @Test
        fun `oppijanumero service returns unidentified user`() {
            // Facade
            val response: TypedResult<HttpResponse<String>, CasError> =
                TypedResult.Success(
                    HttpResponseMock(
                        statusCode = 200,
                        body =
                            """
                            {
                                "oid": "1.2.246.562.24.33342764709",
                                "oppijanumero": ""
                            }
                            """.trimIndent(),
                    ),
                )
            // System under test
            val oppijanumeroService =
                OppijanumeroServiceImpl(
                    casAuthenticatedService =
                        CasAuthenticatedServiceMock(response),
                    objectMapper = ObjectMapper(),
                    tracer = MockTracer(),
                )
            oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

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
            val response: TypedResult<HttpResponse<String>, CasError> =
                TypedResult.Success(
                    HttpResponseMock(
                        statusCode = 404,
                        body =
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
            // System under test
            val oppijanumeroService =
                OppijanumeroServiceImpl(
                    casAuthenticatedService =
                        CasAuthenticatedServiceMock(response),
                    objectMapper = ObjectMapper(),
                    tracer = MockTracer(),
                )
            oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

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
            val response: TypedResult<HttpResponse<String>, CasError> =
                TypedResult.Success(
                    HttpResponseMock(
                        statusCode = 409,
                        body =
                            """
                            {
                                "timestamp": 1734962667439,
                                "status":404,
                                "error":"Conflict",
                                "path":"/oppijanumerorekisteri-service/yleistunniste/hae"
                            }
                            """.trimIndent(),
                    ),
                )
            // System under test
            val oppijanumeroService =
                OppijanumeroServiceImpl(
                    casAuthenticatedService =
                        CasAuthenticatedServiceMock(response),
                    objectMapper = ObjectMapper(),
                    tracer = MockTracer(),
                )
            oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

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
     */
}
