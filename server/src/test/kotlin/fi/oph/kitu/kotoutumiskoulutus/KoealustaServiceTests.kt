package fi.oph.kitu.kotoutumiskoulutus

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(DBContainerConfiguration::class)
class KoealustaServiceTests(
    @param:Autowired private val postgres: PostgreSQLContainer,
) {
    val validSuoritus =
        """
        {
              "userid": 1,
              "firstnames": "Ranja Testi",
              "lastname": "Öhman-Testi",
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
        """.trimIndent()

    val invalidHetu =
        """
        {
          "userid": 2,
          "firstnames": "Antero",
          "lastname": "Testi-Moikka",
          "preferredname": "Antero",
          "SSN": "INVALID_HETU",
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
        """.trimIndent()

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
                      "users": [$validSuoritus]
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
                          "SSN": "WRONG_HETU",
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
            fun() = assertEquals("WRONG_HETU", oppijaValidationFailure.hetu),
            fun() =
                assertEquals(
                    "Oppijanumeron haku epäonnistui: Oppija YleistunnisteHaeRequest(etunimet=Antero, hetu=WRONG_HETU, kutsumanimi=Antero, sukunimi=Testi-Moikka) is not identified in oppijanumero service",
                    oppijaValidationFailure.viesti,
                ),
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
                          "firstnames": "Ranja Testi",
                          "lastname": "Öhman-Testi",
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
            fun() = assertEquals("Öhman-Testi Ranja Testi", missingPuhuminenError.nimi),
            fun() = assertEquals("010180-9026", missingPuhuminenError.hetu),
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
                      "users": [$invalidHetu]
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
            fun() = assertEquals("INVALID_HETU", onrBadRequestFailure.hetu),
            fun() =
                assertEquals(
                    "Oppijanumeron haku epäonnistui: Bad request to oppijanumero-service",
                    onrBadRequestFailure.viesti,
                ),
            fun() = assertEquals("Testi-Moikka Antero", onrBadRequestFailure.nimi),
            fun() = assertEquals("1.2.246.562.10.1234567890", onrBadRequestFailure.schoolOid.toString()),
            fun() = assertEquals("opettaja@testi.oph.fi", onrBadRequestFailure.teacherEmail),
        )
    }

    @Test
    fun `import with valid and invalid suoritus should return original from-timestamp and save the suoritus and error`(
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
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
                      "users": [$validSuoritus, $invalidHetu]
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

        val errors = kielitestiSuoritusErrorRepository.findAll()
        val suoritukset = kielitestiSuoritusRepository.findAll()

        assertAll(
            fun() =
                assertEquals(
                    expected = Instant.EPOCH,
                    actual = lastSeen,
                    message = "Since an error was encountered, the previous `from` parameter should have been returned",
                ),
            fun () = assertEquals(1, errors.count(), "There should be one saved error"),
            fun () = assertEquals(1, suoritukset.count(), "There should be one saved suoritus"),
        )
    }

    @Test
    fun `duplicate suoritus in subsequent imports are not saved`(
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
        @Autowired koealustaService: KoealustaService,
    ) {
        // Facade
        val koealusta = MockRestServiceServer.bindTo(koealustaService.restClientBuilder).build()
        koealusta
            .expect(
                ExpectedCount.manyTimes(),
                requestTo(
                    "https://localhost:8080/dev/koto/webservice/rest/server.php?wstoken=token&wsfunction=local_completion_export_get_completions&moodlewsrestformat=json&from=0",
                ),
            ).andRespond(
                withSuccess(
                    """
                    {
                      "users": [$validSuoritus, $invalidHetu]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        koealustaService.koealustaToken = "token"
        koealustaService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        // Test
        val lastSeen = koealustaService.importSuoritukset(Instant.EPOCH)

        val errors = kielitestiSuoritusErrorRepository.findAll()
        val suoritukset = kielitestiSuoritusRepository.findAll()

        assertAll(
            fun() =
                assertEquals(
                    expected = Instant.EPOCH,
                    actual = lastSeen,
                    message = "Since an error was encountered, the previous `from` parameter should have been returned",
                ),
            fun () = assertEquals(1, errors.count(), "There should be one saved error"),
            fun () = assertEquals(1, suoritukset.count(), "There should be one saved suoritus"),
        )

        koealustaService.importSuoritukset(from = lastSeen)
        koealusta.verify()

        val suoritukset2 = kielitestiSuoritusRepository.findAll()
        val errors2 = kielitestiSuoritusErrorRepository.findAll()
        assertAll(
            fun() = assertEquals(1, suoritukset2.count()),
            fun() = assertEquals(1, errors2.count()),
        )
    }

    @Test
    fun `import removes leading and trailing whitespace from names and ssn`(
        @Autowired kielitestiSuoritusRepository: KielitestiSuoritusRepository,
        @Autowired kielitestiSuoritusErrorRepository: KielitestiSuoritusErrorRepository,
        @Autowired koealustaService: KoealustaService,
    ) {
        // Facade
        val validSuoritus =
            """
            {
                  "userid": 1,
                  "firstnames": " Ranja Testi ",
                  "lastname": " Öhman-Testi ",
                  "preferredname": " Ranja ", 
                  "SSN": " 010180-9026 ",
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
            """.trimIndent()

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
                      "users": [$validSuoritus]
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
            fun () = assertEquals("Ranja Testi", ranja.firstNames),
            fun () = assertEquals("Öhman-Testi", ranja.lastName),
            fun () = assertEquals("Ranja", ranja.preferredname),
            fun () = assertEquals(Oid.parse("1.2.246.562.24.33342764709").getOrThrow(), ranja.oppijanumero),
            fun () = assertEquals(0, kielitestiSuoritusErrorRepository.findAll().count()),
        )
    }
}
