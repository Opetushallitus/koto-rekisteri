package fi.oph.kitu.oppijanumero

import HttpResponseMock
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.assertFailureIsThrowable
import fi.oph.kitu.logging.MockTracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.net.http.HttpResponse
import kotlin.test.assertEquals

class OppijanumeroServiceTests {
    @Test
    fun `oppijanumero service returns identified user`() {
        // Facade
        val expectedOppijanumero = Oid.parse("1.2.246.562.24.33342764709").getOrThrow()
        val response: TypedResult<HttpResponse<String>, CasError> =
            TypedResult.Success(
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
                tracer = MockTracer(),
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
                        "123",
                    ).getOrThrow()
            }
        assertEquals(expectedOppijanumero, result)
    }

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
                    "123",
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
                    "123",
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
                "123",
            )

        assertFailureIsThrowable<OppijanumeroException>(
            result,
            "Oppijanumeron haku epäonnistui (409): Jotkin Moodle-käyttäjän '123' tunnistetiedoista (hetu, etunimet, kutsumanimi, sukunimi) ovat virheellisiä.",
        )
    }
}
