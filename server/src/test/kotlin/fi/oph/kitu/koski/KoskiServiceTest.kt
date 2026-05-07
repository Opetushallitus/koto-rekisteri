package fi.oph.kitu.koski

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.dev.mockdata.generateRandomYkiSuoritusEntity
import fi.oph.kitu.logging.OpenTelemetryTestConfig
import fi.oph.kitu.result.TypedResult
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritusService
import fi.oph.kitu.yki.YkiService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusFilter
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
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
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
@Import(OpenTelemetryTestConfig::class, DBContainerConfiguration::class)
class KoskiServiceTest(
    @param:Autowired private val koskiYkiRequestMapper: KoskiYkiRequestMapper,
    @param:Autowired private val koskiVktRequestMapper: KoskiVktRequestMapper,
    @param:Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
    @param:Autowired private val mockRestClientBuilder: RestClient.Builder,
    @param:Autowired private val tracer: Tracer,
    @param:Autowired private val inMemorySpanExporter: InMemorySpanExporter,
    @param:Autowired private val koskiErrorService: KoskiErrorService,
    @param:Autowired private val postgres: PostgreSQLContainer,
) {
    @Autowired
    private lateinit var vktSuoritusService: VktSuoritusService

    @Autowired
    private lateinit var customVktSuoritusRepository: CustomVktSuoritusRepository

    @Autowired
    private lateinit var vktSuoritusRepository: VktSuoritusRepository

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
        val koskiResponse = successfulKoskiResponseFor(suoritus)
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("oppija"))
            .andRespond(
                withSuccess(
                    koskiResponse,
                    MediaType.APPLICATION_JSON,
                ),
            )

        val service =
            KoskiService(
                mockRestClientBuilder.build(),
                koskiYkiRequestMapper,
                koskiVktRequestMapper,
                ykiSuoritusRepository,
                customVktSuoritusRepository,
                vktSuoritusService,
                koskiErrorService,
            )

        val updatedSuoritus = service.sendYkiSuoritusToKoski(suoritus).getOrThrow()
        assertEquals("1.2.246.562.15.50209741037", updatedSuoritus.koskiOpiskeluoikeus.toString())

        // KoskiService.sendYkiSuoritusToKoski span ei tallennu, koska tämä testi instantioi
        // KoskiServicen käsin ohittaen Springin AOP-proxyn, jonka @WithSpan tarvitsisi.
        // KoskiYkiRequestMapper sen sijaan tulee Springin DI:n kautta, joten @WithSpan toimii sille.
        val spans = inMemorySpanExporter.finishedSpanItems
        assertNotNull(spans)
        assertNotNull(spans.find { it.name == "KoskiYkiRequestMapper.ykiSuoritusToKoskiRequest" })
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
            KoskiService(
                mockRestClientBuilder.build(),
                koskiYkiRequestMapper,
                koskiVktRequestMapper,
                ykiSuoritusRepository,
                customVktSuoritusRepository,
                vktSuoritusService,
                koskiErrorService,
            )
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(id = 1)

        val updatedSuoritus = service.sendYkiSuoritusToKoski(suoritus)
        assertTrue(updatedSuoritus is TypedResult.Failure)
        assertEquals(YkiMappingId(suoritus.solkiId), updatedSuoritus.errorOrNull()?.suoritusId)
    }

    @Test
    fun `test sending all yki suoritukset to KOSKI`() {
        val koskiResponse = successfulKoskiResponseFor("1.2.246.562.24.20281155246", 183424)
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(ExpectedCount.times(3), requestTo("oppija"))
            .andRespond(
                withSuccess(
                    koskiResponse,
                    MediaType.APPLICATION_JSON,
                ),
            )

        val service =
            KoskiService(
                mockRestClientBuilder.build(),
                koskiYkiRequestMapper,
                koskiVktRequestMapper,
                ykiSuoritusRepository,
                customVktSuoritusRepository,
                vktSuoritusService,
                koskiErrorService,
            )

        ykiSuoritusRepository.saveAllNewEntities(
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
        val koskiResponse = successfulKoskiResponseFor("1.2.246.562.24.20281155246", 183424)
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
                    koskiResponse,
                    MediaType.APPLICATION_JSON,
                ),
            )

        val service =
            KoskiService(
                mockRestClientBuilder.build(),
                koskiYkiRequestMapper,
                koskiVktRequestMapper,
                ykiSuoritusRepository,
                customVktSuoritusRepository,
                vktSuoritusService,
                koskiErrorService,
            )

        ykiSuoritusRepository.saveAllNewEntities(
            listOf(
                generateRandomYkiSuoritusEntity(),
                generateRandomYkiSuoritusEntity(),
                generateRandomYkiSuoritusEntity(),
            ),
        )

        service.sendYkiSuorituksetToKoski()

        val updatedSuoritukset = ykiService.allSuoritukset(versionHistory = false)
        assertEquals(3, updatedSuoritukset.size)
        assertEquals(2, updatedSuoritukset.filter { it.koskiOpiskeluoikeus != null }.size)
    }

    @Test
    fun `verify that the correct version of suoritus is processed at every step`() {
        val viimeisinVersio = generateRandomYkiSuoritusEntity()
        val toinenVersio = viimeisinVersio.copy(puhuminen = null)
        val ekaVersio =
            toinenVersio.copy(
                lastModified = viimeisinVersio.lastModified.minusSeconds(1000),
            )

        ykiSuoritusRepository.save(ekaVersio, true)
        ykiSuoritusRepository.save(toinenVersio, true)
        ykiSuoritusRepository.save(viimeisinVersio, true)

        val suoritusKoskeen =
            ykiSuoritusRepository
                .findKoskeenLahettamattomatSuoritukset()
                .find { it.solkiId == ekaVersio.solkiId }!!

        assertEquals(viimeisinVersio.puhuminen, suoritusKoskeen.puhuminen)

        val koskiService = setupKoskiMock(successfulKoskiResponseFor(viimeisinVersio))

        val lahetettyVersio = koskiService.sendYkiSuoritusToKoski(suoritusKoskeen).getOrThrow()
        assertEquals(viimeisinVersio.puhuminen, lahetettyVersio.puhuminen)

        val lahetetyt = ykiSuoritusRepository.findSuorituksetWithKoskiopiskeluoikeus()
        assertEquals(lahetetyt.map { it.solkiId }, listOf(viimeisinVersio.solkiId))
    }

    private fun setupKoskiMock(response: String): KoskiService {
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("oppija"))
            .andRespond(
                withSuccess(
                    response,
                    MediaType.APPLICATION_JSON,
                ),
            )

        return KoskiService(
            mockRestClientBuilder.build(),
            koskiYkiRequestMapper,
            koskiVktRequestMapper,
            ykiSuoritusRepository,
            customVktSuoritusRepository,
            vktSuoritusService,
            koskiErrorService,
        )
    }

    private fun successfulKoskiResponseFor(suoritus: YkiSuoritusEntity): String =
        successfulKoskiResponseFor(suoritus.suorittajanOID.toString(), suoritus.solkiId)

    private fun successfulKoskiResponseFor(
        oppijanumero: String,
        solkiId: Int,
    ): String =
        """
        {
          "henkilö": {
            "oid": "$oppijanumero"
          },
          "opiskeluoikeudet": [
            {
              "oid": "1.2.246.562.15.50209741037",
              "versionumero": 1,
              "lähdejärjestelmänId": {
                "id": "$solkiId",
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
}
