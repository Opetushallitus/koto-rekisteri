package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import fi.oph.kitu.assertFailureIsThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OppijanumeroServiceTests {
    @TestConfiguration
    class RestClientBuilderConfig {
        @Bean
        fun restClientBuilder() = RestClient.builder()
    }

    @TestConfiguration
    class OppijanumeroRestClientConfig(
        private val restClientBuilder: RestClient.Builder,
    ) {
        @Value("\${kitu.oppijanumero.service.url}")
        private lateinit var serviceUrl: String

        @Value("\${kitu.oppijanumero.callerid}")
        private lateinit var callerId: String

        @Primary
        @Bean("oppijanumeroRestClient")
        fun oppijanumeroRestClient(): RestClient =
            restClientBuilder
                .baseUrl(serviceUrl)
                .defaultHeaders { headers ->
                    headers["Caller-Id"] = callerId
                    headers["CSRF"] = "CSRF"
                    headers["Cookie"] = "CSRF=CSRF"
                }.build()
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

    @Test
    fun `oppijanumero service returns identified user`(
        @Autowired oppijanumeroService: OppijanumeroService,
        @Autowired restClientBuilder: RestClient.Builder,
    ) {
        // Facade
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
    fun `oppijanumero service returns unidentified user`(
        @Autowired oppijanumeroService: OppijanumeroService,
        @Autowired restClientBuilder: RestClient.Builder,
    ) {
        // Facade
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
    fun `oppijanumero service does not find user`(
        @Autowired oppijanumeroService: OppijanumeroService,
        @Autowired restClientBuilder: RestClient.Builder,
    ) {
        // Facade
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
    fun `oppijanumero service received bad request`(
        @Autowired oppijanumeroService: OppijanumeroService,
        @Autowired restClientBuilder: RestClient.Builder,
    ) {
        // Facade
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
