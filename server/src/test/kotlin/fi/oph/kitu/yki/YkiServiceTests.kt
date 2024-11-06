package fi.oph.kitu.yki

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class YkiServiceTests(
    @Autowired private val ykiRepository: YkiRepository,
) {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")
    }

    @Test
    fun `test import works`() {
        // Arrange
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.20281155246","010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,5,5,,5,5,
                    "1.2.246.562.24.59267607404","010116A9518","M","Kivinen-Testi","Petro Testi","EST","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,
                    "1.2.246.562.24.74064782358","010100A9846","N","Vesala-Testi","Fanni Testi","EST","Testitie 23","40100","Testinsuu","testi.fanni@testi.fi",2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,4,4,,4,4,
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        // System under test
        val ykiService =
            YkiService(
                solkiRestClient = mockRestClientBuilder.build(),
                repository = ykiRepository,
            )

        // Act
        ykiService.importYkiSuoritukset(null, false)

        // Assert
        val suoritukset = ykiRepository.findAll()
        assertEquals(3, suoritukset.count())
    }

    @Test
    fun `invalid oppijanumero throws exception`() {
        // Arrange
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset"))
            .andRespond(
                withSuccess(
                    """
                    "","010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,5,5,,5,5,
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        // System under test
        val ykiService =
            YkiService(
                solkiRestClient = mockRestClientBuilder.build(),
                repository = ykiRepository,
            )

        // Act
        assertThrows<RestClientException> {
            ykiService.importYkiSuoritukset(null, false)
        }
        val suoritukset = ykiRepository.findAll()
        assertEquals(0, suoritukset.count())
    }
}
