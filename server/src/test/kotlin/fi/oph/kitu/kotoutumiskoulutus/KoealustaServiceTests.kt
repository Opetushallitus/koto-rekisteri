package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.mustBeSuccess
import fi.oph.kitu.oppijanumero.CasAuthenticatedService
import fi.oph.kitu.oppijanumero.CasAuthenticatedServiceMock
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
import org.springframework.http.ResponseEntity
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
                                        hetu = "12345678903",
                                        kutsumanimi = "Antero",
                                        sukunimi = "Testi-Moikka",
                                    ),
                                message = "Bad request",
                                response = ResponseEntity.badRequest().body("Bad request"),
                            ),
                        ),
                    "12345678904" to
                        TypedResult.Failure(
                            OppijanumeroException.UnexpectedError(
                                request =
                                    YleistunnisteHaeRequest(
                                        etunimet = "Antero",
                                        hetu = "12345678904",
                                        kutsumanimi = "Antero",
                                        sukunimi = "Testi-Moikka",
                                    ),
                                message = "Server Error",
                                response = ResponseEntity.internalServerError().body("Server Error"),
                            ),
                        ),
                ),
            )

        @JvmStatic
        fun casAuthenticatedService(): CasAuthenticatedService =
            CasAuthenticatedServiceMock(
                posts =
                    mapOf(
                        CasAuthenticatedServiceMock.toKey(
                            "yleistunniste/hae",
                            YleistunnisteHaeRequest(
                                etunimet = "Ranja",
                                hetu = "010180-9026",
                                kutsumanimi = "Ranja Testi",
                                sukunimi = "Öhman-Testi",
                            ),
                            contentType = MediaType.APPLICATION_JSON,
                            responseType = String::class.java,
                        ) to
                            TypedResult.Success(
                                ResponseEntity.ok().body(
                                    """
                                    {
                                        "oid": "1.2.246.562.24.33342764709",
                                        "oppijanumero:  "1.2.246.562.24.33342764709"
                                    }
                                    """.trimIndent(),
                                ),
                            ),
                    ),
            )
    }

    // @TestBean
    // @Suppress("unused")
    // private lateinit var oppijanumeroService: OppijanumeroService

    @TestBean
    @Suppress("unused")
    private lateinit var casAuthenticatedService: CasAuthenticatedService

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
                          "SSN": "010180-9026",
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

        assertAll(
            fun () = assertEquals(emptyList(), kielitestiSuoritusErrorRepository.findAll()),
            fun () = assertEquals(Instant.parse("2024-10-15T05:12:11Z"), lastSeen),
        )

        val ranja = kielitestiSuoritusRepository.findById(1).get()

        assertAll(
            fun () = assertEquals("Integraatio testaus", ranja.coursename),
            fun () = assertEquals(Oid.parse("1.2.246.562.10.1234567890").getOrThrow(), ranja.schoolOid),
            fun () = assertEquals(Oid.parse("1.2.246.562.24.33342764709").getOrThrow(), ranja.oppijanumero),
            fun () = assertEquals(0, kielitestiSuoritusErrorRepository.findAll().count()),
        )
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

        assertEquals(1, errors.size)

        val oppijaValidationFailure = errors.first()

        assertAll(
            fun() = assertEquals("12345678902", oppijaValidationFailure.hetu),
            fun() =
                assertEquals(
                    "Oppijanumeron haku epäonnistui: virheviesti",
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
            fun() = assertEquals("1.2.246.562.24.33342764709", missingPuhuminenError.suorittajanOid),
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
        assertEquals(1, errors.size)
        val oppijaValidationFailure = errors.first()

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
        assertEquals(1, errors.size)
        val onrBadRequestFailure = errors.first()

        assertAll(
            fun() = assertEquals("12345678903", onrBadRequestFailure.hetu),
            fun() =
                assertEquals(
                    "Oppijanumeron haku epäonnistui: Bad request",
                    onrBadRequestFailure.viesti,
                ),
            fun() = assertEquals("Testi-Moikka Antero", onrBadRequestFailure.nimi),
            fun() = assertEquals("1.2.246.562.10.1234567890", onrBadRequestFailure.schoolOid.toString()),
            fun() = assertEquals("opettaja@testi.oph.fi", onrBadRequestFailure.teacherEmail),
        )
    }
}
