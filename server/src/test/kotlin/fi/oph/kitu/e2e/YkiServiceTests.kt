package fi.oph.kitu.e2e

import fi.oph.kitu.dev.YkiController
import fi.oph.kitu.yki.YkiService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.format.DateTimeFormatter

// @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
// @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest
@Testcontainers
class YkiServiceTests(
    @Autowired val ykiService: YkiService,
    @Autowired val ykiDevController: YkiController,
) {
    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")!!
    }

    @MockitoBean
    @Qualifier("solkiRestClient")
    private lateinit var solkiRestClient: RestClient

    // @Order(1)
    @Test
    fun `Happy path for Yki suoritukset import`() {
        val request = mock(RestClient.RequestHeadersUriSpec::class.java)
        val responseSpec = mock(RestClient.ResponseSpec::class.java)
        val entityResponse = mock(ResponseEntity::class.java) as ResponseEntity<String>

        val from = Instant.now()
        val url = "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}"

        whenever(solkiRestClient.get()).thenReturn(request)
        whenever(request.uri("/some-endpoint")).thenReturn(request)
        whenever(request.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.toEntity(String::class.java)).thenReturn(entityResponse)
        whenever(entityResponse.body).thenReturn("mocked response")

        ykiService.importYkiSuoritukset(from)
    }

    // @Order(2)
    @Test
    fun `two bad row makes YKi suoritukset response throw an exception`() {
        ykiDevController.setResponse(
            endpoint = "/yki/import/suoritukset",
            response =
                ResponseEntity.ok(
                    """
                    "1.2.246.562.24.20281155246","010180-9026","N","Öhman-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
                    "1.2.246.562.24.59267607404","010116A9518","M","Kivinen-Testi","Petro Testi","EST","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-10-30T13:55:09Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,
                    "1.2.246.562.24.74064782358","010100A9846","N","Vesala-Testi","Fanni Testi","EST","Testitie 23","40100","Testinsuu","testi.fanni@testi.fi",183426,2024-10-30T13:55:47Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,4,4,,4,4,,,,0,0,,
                    """.trimIndent(),
                ),
        )

        ykiService.importYkiSuoritukset(Instant.now())
    }
}
