package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.oppijanumero.OppijanumeroServiceMock
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
class KoealustaServiceTests(
    @Autowired private val auditLogger: AuditLogger,
) {
    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Test
    fun `import with no errors`(
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
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
                          "firstnames": "Ranja Testi",
                          "lastname": "\u00f6hman-Testi",
                          "preferredname": "Ranja", 
                          "oppijanumero": "1.2.246.562.24.33342764709",
                          "SSN": "12345678901",
                          "email": "ranja.testi@oph.fi",
                          "completions": [
                            {
                              "courseid": 32,
                              "coursename": "Integraatio testaus",
                              "schoolOID": "1.2.246.562.10.1234567890",
                              "results": [
                                {
                                  "name": "luetun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": "A1",
                                  "quiz_result_teacher": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": "B1",
                                  "quiz_result_teacher": "B1"
                                },
                                {
                                  "name": "puhe",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": "A1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": "A1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "total_evaluation_teacher": "47,5",
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
                mappingService =
                    KoealustaMappingService(
                        objectMapper,
                        OppijanumeroServiceMock(mapOf("12345678901" to "1.2.246.562.24.33342764709")),
                    ),
                auditLogger = auditLogger,
                kielitestiSuoritusErrorRepository = kielitestiSuoritusErrorRepository,
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

        val ranja = kielitestiSuoritusRepository.findById(1).get()

        assertEquals(
            expected = "Integraatio testaus",
            actual = ranja.coursename,
        )
        assertEquals(
            expected = Oid.parse("1.2.246.562.10.1234567890").getOrThrow(),
            actual = ranja.schoolOid,
        )
    }

    @Test
    fun `import with invalid hetu`(
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
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
                          "firstnames": "Ranja Testi",
                          "lastname": "\u00f6hman-Testi",
                          "preferredname": "Ranja", 
                          "oppijanumero": "1.2.246.562.24.33342764709",
                          "SSN": "12345678901",
                          "email": "ranja.testi@oph.fi",
                          "completions": [
                            {
                              "courseid": 32,
                              "coursename": "Integraatio testaus",
                              "schoolOID": "1.2.246.562.10.1234567890",
                              "results": [
                                {
                                  "name": "luetun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": "A1",
                                  "quiz_result_teacher": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": "B1",
                                  "quiz_result_teacher": "B1"
                                },
                                {
                                  "name": "puhe",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": "A1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": "A1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "total_evaluation_teacher": "47,5",
                              "total_evaluation_system": "40"
                            }
                          ]
                        },
                        {
                          "firstnames": "Ranja Testi",
                          "lastname": "\u00f6hman-Testi",
                          "preferredname": "Ranja", 
                          "SSN": "12341678901",
                          "email": "unknown-ssn@oph.fi",
                          "completions": [
                            {
                              "courseid": 32,
                              "coursename": "Integraatio testaus",
                              "schoolOID": "1.2.246.562.10.1234567890",
                              "results": [
                                {
                                  "name": "luetun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": "A1",
                                  "quiz_result_teacher": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_result_system": "B1",
                                  "quiz_result_teacher": "B1"
                                },
                                {
                                  "name": "puhe",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": "A1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_result_system": null,
                                  "quiz_result_teacher": "A1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "total_evaluation_teacher": "47,5",
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
                mappingService =
                    KoealustaMappingService(
                        objectMapper,
                        OppijanumeroServiceMock(mapOf("12345678901" to "1.2.246.562.24.33342764709")),
                    ),
                auditLogger = auditLogger,
                kielitestiSuoritusErrorRepository = kielitestiSuoritusErrorRepository,
            )

        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        // Verification
        mockServer.verify()

        assertEquals(
            expected = Instant.parse("1970-01-01T00:00:00Z"),
            actual = lastSeen,
            message = "since an error was encountered, use the previous `from` parameter as the last seen date",
        )

        val ranja = kielitestiSuoritusRepository.findById(1).get()

        assertEquals(
            expected = "Integraatio testaus",
            actual = ranja.coursename,
        )
        assertEquals(
            expected = Oid.parse("1.2.246.562.10.1234567890").getOrThrow(),
            actual = ranja.schoolOid,
        )

        val error = kielitestiSuoritusErrorRepository.findById(1).get()
        assertEquals("foo", error.hetu)
        assertEquals("foo", error.virheellinenKentta)
        assertEquals("foo", error.virheellinenArvo)
    }
}
