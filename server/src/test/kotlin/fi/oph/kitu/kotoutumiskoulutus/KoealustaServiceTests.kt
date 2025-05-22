package fi.oph.kitu.kotoutumiskoulutus

import HttpResponseMock
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.mustBeSuccess
import fi.oph.kitu.oppijanumero.OppijanumeroException
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.oppijanumero.OppijanumeroServiceMock
import fi.oph.kitu.oppijanumero.YleistunnisteHaeRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Testcontainers
class KoealustaServiceTests {
    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16")

        @JvmStatic
        fun oppijanumeroService(): OppijanumeroService =
            OppijanumeroServiceMock(
                mapOf(
                    "12345678901" to Oid.parseTyped("1.2.246.562.24.33342764709").mustBeSuccess(),
                    "12345678902" to
                        TypedResult.Failure(
                            OppijanumeroException.OppijaNotIdentifiedException(
                                request =
                                    YleistunnisteHaeRequest(
                                        etunimet = "Antero",
                                        hetu = "12345678902",
                                        kutsumanimi = "Antero",
                                        sukunimi = "Testi-Moikka",
                                    ),
                                message = "virheviesti",
                            ),
                        ),
                    "12345678903" to
                        TypedResult.Failure(
                            OppijanumeroException.BadRequest(
                                request =
                                    YleistunnisteHaeRequest(
                                        etunimet = "Antero",
                                        hetu = "12345678902",
                                        kutsumanimi = "Antero",
                                        sukunimi = "Testi-Moikka",
                                    ),
                                message = "Bad request",
                                response = HttpResponseMock(400, "Bad request"),
                            ),
                        ),
                    "12345678904" to
                        TypedResult.Failure(
                            OppijanumeroException.UnexpectedError(
                                request =
                                    YleistunnisteHaeRequest(
                                        etunimet = "Antero",
                                        hetu = "12345678902",
                                        kutsumanimi = "Antero",
                                        sukunimi = "Testi-Moikka",
                                    ),
                                message = "Server Error",
                                response = HttpResponseMock(500, "Server Error"),
                            ),
                        ),
                ),
            )
    }

    @TestBean
    @Suppress("unused")
    private lateinit var oppijanumeroService: OppijanumeroService

    @BeforeEach
    fun nukeDb(
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
    ) {
        kielitestiSuoritusRepository.deleteAll()
        kielitestiSuoritusErrorRepository.deleteAll()
    }

    @Test
    fun `import with no errors`(
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired koealustaService: KoealustaService,
    ) {
        // Facade
        val mockServer = MockRestServiceServer.bindTo(koealustaService.restClientBuilder).build()
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
                          "userid": 1,
                          "firstnames": "Ranja Testi",
                          "lastname": "\u00f6hman-Testi",
                          "preferredname": "Ranja", 
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
                                  "quiz_grade": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "puhuminen",
                                  "quiz_grade": "A1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_grade": "A1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "teacheremail": "opettaja@testi.oph.fi"
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
        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        // Verification
        mockServer.verify()

        assertEquals(emptyList(), kielitestiSuoritusErrorRepository.findAll())

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

        assertEquals(
            expected = Oid.parse("1.2.246.562.24.33342764709").getOrThrow(),
            actual = ranja.oppijanumero,
        )

        assertEquals(0, kielitestiSuoritusErrorRepository.findAll().count())
    }

    @Test
    fun `import with hetu name mismatch`(
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired koealustaService: KoealustaService,
    ) {
        // Facade
        val koealusta = MockRestServiceServer.bindTo(koealustaService.restClientBuilder).build()
        koealusta
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
                          "userid": 1,
                          "firstnames": "Antero",
                          "lastname": "Testi-Moikka",
                          "preferredname": "Antero", 
                          "SSN": "12345678902",
                          "email": "ranja.testi@oph.fi",
                          "completions": [
                            {
                              "courseid": 32,
                              "coursename": "Integraatio testaus",
                              "schoolOID": "1.2.246.562.10.1234567890",
                              "results": [
                                {
                                  "name": "luetun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "puhuminen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_grade": "B1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "teacheremail": "opettaja@testi.oph.fi"
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        // Verification
        koealusta.verify()

        assertEquals(
            expected = Instant.parse("1970-01-01T00:00:00Z"),
            actual = lastSeen,
            message = "since an error was encountered, use the previous `from` parameter as the last seen date",
        )

        val errors = kielitestiSuoritusErrorRepository.findAll().toList()

        // Jos emme saa ONR:stä oppijanumeroa, niin validaatiologiikka tuottaa virheen sekä oppijalle että jokaiselle suoritukselle.

        val suoritusValidationFailure = errors[0]
        val oppijaValidationFailure = errors[1]

        assertAll(
            fun() = assertEquals("""Missing student "oppijanumero" for user "1"""", suoritusValidationFailure.viesti),
            fun() = assertEquals("12345678902", suoritusValidationFailure.hetu),
            fun() = assertEquals("oppijanumero", suoritusValidationFailure.virheellinenKentta, "virheellinen kenttä"),
            fun() = assertEquals(null, suoritusValidationFailure.virheellinenArvo, "virheellinen arvo"),
            fun() = assertEquals("Testi-Moikka Antero", suoritusValidationFailure.nimi),
        )

        assertAll(
            fun() =
                assertEquals(
                    "Oppijanumeron haku epäonnistui: Jotkin Moodle-käyttäjän tunnistetiedoista (hetu, etunimet, kutsumanimi, sukunimi) ovat virheellisiä. (virheviesti)",
                    oppijaValidationFailure.viesti,
                ),
            fun() = assertEquals("Testi-Moikka Antero", oppijaValidationFailure.nimi),
        )
    }

    @Test
    fun `import with suoritus validation error`(
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired koealustaService: KoealustaService,
    ) {
        // Facade
        val koealusta = MockRestServiceServer.bindTo(koealustaService.restClientBuilder).build()
        koealusta
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
                          "userid": 1,
                          "firstnames": "Antero",
                          "lastname": "Testi-Moikka",
                          "preferredname": "Antero", 
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
                                  "quiz_grade": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_grade": "B1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "teacheremail": "opettaja@testi.oph.fi"
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        // Verification
        koealusta.verify()

        val errors = kielitestiSuoritusErrorRepository.findAll().toList()

        assertEquals(1, errors.size)

        val missingPuhuminenError = errors[0]
        assertAll(
            fun() = assertEquals("Testi-Moikka Antero", missingPuhuminenError.nimi),
            fun() = assertEquals("12345678901", missingPuhuminenError.hetu),
            fun() = assertEquals("1.2.246.562.10.1234567890", missingPuhuminenError.schoolOid.toString()),
            fun() = assertEquals("opettaja@testi.oph.fi", missingPuhuminenError.teacherEmail),
            fun() = assertEquals("puhuminen", missingPuhuminenError.virheellinenKentta),
            fun() =
                assertEquals(
                    "Unexpectedly missing quiz grade \"puhuminen\" on course \"Integraatio testaus\" for user \"1\"",
                    missingPuhuminenError.viesti,
                ),
        )
    }

    @Test
    fun `import with oppija validation error`(
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired koealustaService: KoealustaService,
    ) {
        // Facade
        val koealusta = MockRestServiceServer.bindTo(koealustaService.restClientBuilder).build()
        koealusta
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
                          "userid": 1,
                          "firstnames": "Antero",
                          "lastname": "Testi-Moikka",
                          "preferredname": "Antero",
                          "SSN": "",
                          "email": "ranja.testi@oph.fi",
                          "completions": [
                            {
                              "courseid": 32,
                              "coursename": "Integraatio testaus",
                              "schoolOID": "1.2.246.562.10.1234567890",
                              "results": [
                                {
                                  "name": "luetun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "puhuminen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_grade": "B1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "teacheremail": "opettaja@testi.oph.fi"
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        // Verification
        koealusta.verify()

        val errors = kielitestiSuoritusErrorRepository.findAll().toList()
        val oppijaValidationFailure = errors[0]

        assertAll(
            fun() = assertEquals("1.2.246.562.10.1234567890", oppijaValidationFailure.schoolOid.toString()),
            fun() = assertEquals("opettaja@testi.oph.fi", oppijaValidationFailure.teacherEmail),
            fun() = assertEquals("Missing student \"SSN\" for user \"1\"", oppijaValidationFailure.viesti),
            fun() = assertEquals("SSN", oppijaValidationFailure.virheellinenKentta),
        )
    }

    @Test
    fun `import with person information mismatch`(
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired koealustaService: KoealustaService,
    ) {
        // Facade
        val koealusta = MockRestServiceServer.bindTo(koealustaService.restClientBuilder).build()
        koealusta
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
                          "userid": 1,
                          "firstnames": "Antero",
                          "lastname": "Testi-Moikka",
                          "preferredname": "Antero", 
                          "SSN": "12345678903",
                          "email": "ranja.testi@oph.fi",
                          "completions": [
                            {
                              "courseid": 32,
                              "coursename": "Integraatio testaus",
                              "schoolOID": "1.2.246.562.10.1234567890",
                              "results": [
                                {
                                  "name": "luetun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "A1"
                                },
                                {
                                  "name": "kuullun ymm\u00e4rt\u00e4minen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "puhuminen",
                                  "quiz_grade": "B1"
                                },
                                {
                                  "name": "kirjoittaminen",
                                  "quiz_grade": "B1"
                                }
                              ],
                              "timecompleted": 1728969131,
                              "teacheremail": "opettaja@testi.oph.fi"
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        // Verification
        koealusta.verify()

        assertEquals(
            expected = Instant.parse("1970-01-01T00:00:00Z"),
            actual = lastSeen,
            message = "since an error was encountered, use the previous `from` parameter as the last seen date",
        )

        val errors = kielitestiSuoritusErrorRepository.findAll().toList()

        // Jos emme saa ONR:stä oppijanumeroa, niin validaatiologiikka tuottaa virheen sekä oppijalle että jokaiselle suoritukselle.

        val suoritusValidationFailure = errors[0]
        val onrBadRequestFailure = errors[1]

        assertAll(
            fun() = assertEquals("""Missing student "oppijanumero" for user "1"""", suoritusValidationFailure.viesti),
            fun() = assertEquals("12345678903", suoritusValidationFailure.hetu),
            fun() = assertEquals("oppijanumero", suoritusValidationFailure.virheellinenKentta, "virheellinen kenttä"),
            fun() = assertEquals(null, suoritusValidationFailure.virheellinenArvo, "virheellinen arvo"),
            fun() = assertEquals("Testi-Moikka Antero", suoritusValidationFailure.nimi),
            fun() = assertEquals("1.2.246.562.10.1234567890", suoritusValidationFailure.schoolOid.toString()),
            fun() = assertEquals("opettaja@testi.oph.fi", suoritusValidationFailure.teacherEmail),
        )

        assertAll(
            fun() =
                assertEquals(
                    "Oppijanumeron haku epäonnistui: Jotkin Moodle-käyttäjän tunnistetiedoista (hetu, etunimet, kutsumanimi, sukunimi) ovat virheellisiä. (Bad request)",
                    onrBadRequestFailure.viesti,
                ),
            fun() = assertEquals("Testi-Moikka Antero", onrBadRequestFailure.nimi),
        )
    }
}
