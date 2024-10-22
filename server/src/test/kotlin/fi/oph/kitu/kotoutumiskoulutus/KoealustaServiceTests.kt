package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest
class KoealustaServiceTests {
    @Autowired
    private lateinit var jacksonObjectMapper: ObjectMapper

    @Autowired
    private lateinit var kielitestiSuoritusRepository: KielitestiSuoritusRepository

    @Test
    fun testImportWorks() {
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
                          "firstname": "Mervi",
                          "lastname": "Esimerkki",
                          "OIDnumber": "12345",
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
                jacksonObjectMapper = jacksonObjectMapper,
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
