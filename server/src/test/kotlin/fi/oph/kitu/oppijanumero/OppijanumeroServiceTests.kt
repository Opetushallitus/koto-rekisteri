package fi.oph.kitu.oppijanumero

import HttpResponseMock
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
class OppijanumeroServiceTests {
    @Test
    fun `oppijanumero service returns identified user`(
        @Autowired objectMapper: ObjectMapper,
    ) {
        // Facade
        val response =
            Result.success(
                HttpResponseMock(
                    statusCode = 200,
                    body =
                        """
                        {
                            "oid": "1.2.246.562.24.33342764709",
                            "oppijanumero": "1.2.246.562.24.33342764709"
                        }
                        """.trimIndent(),
                ),
            )
        // System under test
        val oppijanumeroService =
            OppijanumeroServiceImpl(
                casAuthenticatedService =
                    CasAuthenticatedServiceMock(response),
                objectMapper = objectMapper,
            )
        oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

        oppijanumeroService.getOppijanumero(
            Oppija(
                "Magdalena Testi",
                "Sallinen-Testi",
                "Magdalena",
                "010866-9260",
            ),
        )
    }

    @Test
    fun `oppijanumero service returns unidentified user`(
        @Autowired objectMapper: ObjectMapper,
    ) {
        // Facade
        val response =
            Result.success(
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
                objectMapper = objectMapper,
            )
        oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

        assertThrows<OppijanumeroException> {
            oppijanumeroService.getOppijanumero(
                Oppija(
                    "Magdalena Testi",
                    "Sallinen-Testi",
                    "Magdalena",
                    "010866-9260",
                ),
            )
        }
    }

    @Test
    fun `oppijanumero service returns error`(
        @Autowired objectMapper: ObjectMapper,
    ) {
        // Facade
        val response =
            Result.success(
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
                objectMapper = objectMapper,
            )
        oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

        assertThrows<OppijanumeroException> {
            oppijanumeroService.getOppijanumero(
                Oppija(
                    "Magdalena Testi",
                    "Sallinen-Testi",
                    "Magdalena",
                    "010866-9260",
                ),
            )
        }
    }
}
