package fi.oph.kitu.oppijanumero

import HttpResponseMock
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class OppijanumeroServiceTests {
    @Test
    fun `oppijanumero service returns identified user`() {
        // Facade
        val expectedOppijanumero = Oid.parse("1.2.246.562.24.33342764709").getOrThrow()
        val response =
            Result.success(
                HttpResponseMock(
                    statusCode = 200,
                    body =
                        """
                        {
                            "oid": "1.2.246.562.24.33342764709",
                            "oppijanumero": "$expectedOppijanumero"
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
            )
        oppijanumeroService.serviceUrl = "http://localhost:8080/oppijanumero-service"

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
        assertEquals(expectedOppijanumero.toString(), result)
    }

    @Test
    fun `oppijanumero service returns unidentified user`() {
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
                objectMapper = ObjectMapper(),
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
    fun `oppijanumero service returns error`() {
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
                objectMapper = ObjectMapper(),
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
}
