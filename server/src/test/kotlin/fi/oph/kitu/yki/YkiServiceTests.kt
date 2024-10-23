package fi.oph.kitu.yki

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

@SpringBootTest
@Testcontainers
class YkiServiceTests(
    @Autowired private val ykiRepository: YkiRepository,
) {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Test
    fun testImportWorks() {
        // Arrange
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            // TODO: real address for Solki API
            .expect(requestTo("suoritukset"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.99999999999","Suorittaja","Sulevi",2022-11-12,"fin","KT","1.2.246.562.10.373218511910","Iisalmen kansalaisopisto",2,2,1,3,2,2
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
        // TODO: Don't use dry-run, because with dry-run you don't test ykiRepository
        ykiService.importYkiSuoritukset(null, true)

        // Assert
        // No error was thrown
    }
}
