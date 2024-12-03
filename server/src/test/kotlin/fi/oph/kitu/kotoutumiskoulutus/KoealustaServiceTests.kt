package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.oppijanumero.OppijanumeroServiceMock
import fi.oph.kitu.oppijanumero.YleistunnisteHaeResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class KoealustaServiceTests {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Test
    fun `test import works`(
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
        @Autowired objectMapper: ObjectMapper,
    ) {
        // Facade
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(
                requestTo(
                    "https://localhost:8080/dev/koto/webservice/rest/server.php?wstoken=token&wsfunction=local_completion_export_get_completions&moodlewsrestformat=json&from=0",
                ),
            ).andRespond(
                withSuccess(
                    """
                    {
                      "users": [
                        {
                          "firstname": "Mervi-Marianne",
                          "lastname": "Esimerkki",
                          "preferredname": "Mervi", 
                          "OID": "",
                          "SSN": "",
                          "email": "mervi.esimerkki@oph.fi",
                          "completions": [
                            {
                              "courseid": 32,
                              "coursename": "Integraatio testaus",
                              "results": [
                                {
                                  "name": "luetun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": 40,
                                  "quiz_result_teacher": 50
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": 32,
                                  "quiz_result_teacher": 40
                                },
                                {
                                  "name": "puhe",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": 35
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": 55
                                }
                              ],
                              "timecompleted": 1728969131,
                              "total_evaluation_teacher": "47.5",
                              "total_evaluation_system": "40"
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        // System under test
        val koealustaService =
            KoealustaService(
                restClientBuilder = mockRestClientBuilder,
                kielitestiSuoritusRepository = kielitestiSuoritusRepository,
                jacksonObjectMapper = objectMapper,
                oppijanumeroService =
                    OppijanumeroServiceMock(
                        statusCode = 200,
                        response = YleistunnisteHaeResponse("123", "123"),
                    ),
            )

        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        // Verification
        mockServer.verify()

        assertEquals(
            expected = Instant.parse("2024-10-15T05:12:11Z"),
            actual = lastSeen,
        )

        val mervi = kielitestiSuoritusRepository.findById(1).get()

        assertEquals(
            expected = "Integraatio testaus",
            actual = mervi.coursename,
        )
    }
}
