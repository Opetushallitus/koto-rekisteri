package fi.oph.kitu.koski

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.OpenTelemetryTestConfig
import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.YkiService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class KoskiServiceTest(
    @Autowired private val koskiRequestMapper: KoskiRequestMapper,
    @Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
    @Autowired private val mockRestClientBuilder: RestClient.Builder,
    @Autowired private val tracer: Tracer,
    @Autowired private val inMemorySpanExporter: InMemorySpanExporter,
    @Autowired private val postgres: PostgreSQLContainer<*>,
) {
    @Autowired
    private lateinit var ykiService: YkiService

    @BeforeEach
    fun nukeDb() {
        ykiSuoritusRepository.deleteAll()
        inMemorySpanExporter.reset()
    }

    @Test
    fun `test sending koski request`() {
        // Arrange
        val suoritus = generateRandomYkiSuoritusEntity()
        val expectedResponse =
            """
            {
              "henkilö": {
                "oid": "${suoritus.suorittajanOID}"
              },
              "opiskeluoikeudet": [
                {
                  "oid": "1.2.246.562.15.50209741037",
                  "versionumero": 1,
                  "lähdejärjestelmänId": {
                    "id": "${suoritus.suoritusId}",
                    "lähdejärjestelmä": {
                      "koodiarvo": "kielitutkintorekisteri",
                      "nimi": {
                        "fi": "Kielitutkintorekisteri"
                      },
                      "koodistoUri": "lahdejarjestelma",
                      "koodistoVersio": 1
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("oppija"))
            .andRespond(
                withSuccess(
                    expectedResponse,
                    MediaType.APPLICATION_JSON,
                ),
            )

        val service =
            KoskiService(mockRestClientBuilder.build(), koskiRequestMapper, ykiSuoritusRepository, tracer)
        val updatedSuoritus = service.sendYkiSuoritusToKoski(suoritus).getOrThrow()
        assertEquals("1.2.246.562.15.50209741037", updatedSuoritus.koskiOpiskeluoikeus.toString())

        val spans = inMemorySpanExporter.finishedSpanItems
        assertNotNull(spans)
        assertNotNull(spans.find { it.name == "KoskiService.sendYkiSuoritusToKoski" })
        assertNotNull(spans.find { it.name == "KoskiRequestMapper.ykiSuoritusToKoskiRequest" })
    }

    @Test
    fun `test failed koski request`() {
        // Arrange
        val expectedResponse =
            """
            [{"key": "notFound.oppijaaEiLöydy","message": "Oppijaa 1.2.246.562.24.00000000000 ei löydy."}]
            """.trimIndent()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("oppija"))
            .andRespond(
                withBadRequest().body(expectedResponse),
            )

        val service =
            KoskiService(mockRestClientBuilder.build(), koskiRequestMapper, ykiSuoritusRepository, tracer)
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(id = 1)

        val updatedSuoritus = service.sendYkiSuoritusToKoski(suoritus)
        assertTrue(updatedSuoritus is TypedResult.Failure)
        assertEquals(suoritus.id, updatedSuoritus.error.suoritusId)
    }

    @Test
    fun `test sending all yki suoritukset to KOSKI`() {
        val expectedResponse =
            """
            {
              "henkilö": {
                "oid": "1.2.246.562.24.20281155246"
              },
              "opiskeluoikeudet": [
                {
                  "oid": "1.2.246.562.15.50209741037",
                  "versionumero": 1,
                  "lähdejärjestelmänId": {
                    "id": "183424",
                    "lähdejärjestelmä": {
                      "koodiarvo": "kielitutkintorekisteri",
                      "nimi": {
                        "fi": "Kielitutkintorekisteri"
                      },
                      "koodistoUri": "lahdejarjestelma",
                      "koodistoVersio": 1
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(ExpectedCount.times(3), requestTo("oppija"))
            .andRespond(
                withSuccess(
                    expectedResponse,
                    MediaType.APPLICATION_JSON,
                ),
            )
        val service =
            KoskiService(mockRestClientBuilder.build(), koskiRequestMapper, ykiSuoritusRepository, tracer)

        ykiSuoritusRepository.saveAll(
            listOf(
                generateRandomYkiSuoritusEntity(),
                generateRandomYkiSuoritusEntity(),
                generateRandomYkiSuoritusEntity(),
            ),
        )
        service.sendYkiSuorituksetToKoski()
        val updatedSuoritukset = ykiService.allSuoritukset(versionHistory = false)
        assertEquals(3, updatedSuoritukset.size)
        updatedSuoritukset.forEach {
            assertEquals(true, it.koskiSiirtoKasitelty)
            assertEquals("1.2.246.562.15.50209741037", it.koskiOpiskeluoikeus.toString())
        }
    }

    @Test
    fun `test failing to send one suoritus to KOSKI throws error and saves succesfull ones`() {
        val expectedResponse =
            """
            {
              "henkilö": {
                "oid": "1.2.246.562.24.20281155246"
              },
              "opiskeluoikeudet": [
                {
                  "oid": "1.2.246.562.15.50209741037",
                  "versionumero": 1,
                  "lähdejärjestelmänId": {
                    "id": "183424",
                    "lähdejärjestelmä": {
                      "koodiarvo": "kielitutkintorekisteri",
                      "nimi": {
                        "fi": "Kielitutkintorekisteri"
                      },
                      "koodistoUri": "lahdejarjestelma",
                      "koodistoVersio": 1
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("oppija"))
            .andRespond(
                withBadRequest().body(
                    "[{\"key\": \"notFound.oppijaaEiLöydy\",\"message\": \"Oppijaa 1.2.246.562.24.00000000000 ei löydy.\"}]",
                ),
            )
        mockServer
            .expect(ExpectedCount.times(2), requestTo("oppija"))
            .andRespond(
                withSuccess(
                    expectedResponse,
                    MediaType.APPLICATION_JSON,
                ),
            )

        val service =
            KoskiService(mockRestClientBuilder.build(), koskiRequestMapper, ykiSuoritusRepository, tracer)

        ykiSuoritusRepository.saveAll(
            listOf(
                generateRandomYkiSuoritusEntity(),
                generateRandomYkiSuoritusEntity(),
                generateRandomYkiSuoritusEntity(),
            ),
        )

        assertFailsWith<KoskiService.Error.SendToKOSKIFailed> { service.sendYkiSuorituksetToKoski() }

        val updatedSuoritukset = ykiService.allSuoritukset(versionHistory = false)
        assertEquals(3, updatedSuoritukset.size)
        assertEquals(2, updatedSuoritukset.filter { it.koskiOpiskeluoikeus != null }.size)
    }
}
