package fi.oph.kitu.koski

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.Tutkintokieli
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
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class KoskiServiceTest(
    @Autowired private val koskiRequestMapper: KoskiRequestMapper,
) {
    private val objectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")!!
    }

    @Test
    fun `test sending koski request`() {
        // Arrange
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
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("oppija"))
            .andRespond(
                withSuccess(
                    expectedResponse,
                    MediaType.APPLICATION_JSON,
                ),
            )

        val service = KoskiService(mockRestClientBuilder.build(), koskiRequestMapper)
        val suoritus = generateRandomYkiSuoritusEntity().copy(tutkintokieli = Tutkintokieli.ENG)
        val response = service.sendYkiSuoritusToKoski(suoritus)
        assertEquals(objectMapper.readValue(expectedResponse, KoskiResponse::class.java), response)
    }
}
