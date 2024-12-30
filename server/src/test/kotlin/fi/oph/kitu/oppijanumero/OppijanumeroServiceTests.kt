package fi.oph.kitu.oppijanumero

import HttpResponseMock
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.logging.MockEvent
import fi.oph.kitu.logging.MockLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class OppijanumeroServiceTests {
    @Test
    fun `oppijanumero service returns identified user`() {
        // Facade
        val event = MockEvent()
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
                objectMapper = ObjectMapper(),
                logger = MockLogger(event),
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

        val isResponseSuccess = event.getValueOrNullByKey<Boolean>("oppijanumero.parse-response.success") == true
        assertTrue(isResponseSuccess)
    }

    @Test
    fun `oppijanumero service returns unidentified user`() {
        // Facade
        val event = MockEvent()
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
                objectMapper = ObjectMapper(),
                logger = MockLogger(event),
            )
        oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

        assertThrows<OppijanumeroException.OppijaNotIdentifiedException> {
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
    fun `oppijanumero service returns error`() {
        // Facade
        val event = MockEvent()
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
                objectMapper = ObjectMapper(),
                logger = MockLogger(event),
            )
        oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

        assertThrows<OppijanumeroException.OppijaNotFoundException> {
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
