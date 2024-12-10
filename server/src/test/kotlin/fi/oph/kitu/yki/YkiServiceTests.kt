package fi.oph.kitu.yki

import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
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
import java.lang.RuntimeException
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class YkiServiceTests(
    @Autowired private val ykiSuoritusRepository: YkiSuoritusRepository,
    @Autowired private val ykiArvioijaRepository: YkiArvioijaRepository,
) {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")
    }

    @BeforeEach
    fun nukeDb() {
        ykiArvioijaRepository.deleteAll()
        ykiSuoritusRepository.deleteAll()
    }

    @Test
    fun `test suoritukset import works`() {
        // Arrange
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.20281155246","010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
                    "1.2.246.562.24.59267607404","010116A9518","M","Kivinen-Testi","Petro Testi","EST","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-10-30T13:55:09Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,
                    "1.2.246.562.24.74064782358","010100A9846","N","Vesala-Testi","Fanni Testi","EST","Testitie 23","40100","Testinsuu","testi.fanni@testi.fi",183426,2024-10-30T13:55:47Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,4,4,,4,4,,,,0,0,,
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        // System under test
        val ykiService =
            YkiService(
                solkiRestClient = mockRestClientBuilder.build(),
                suoritusRepository = ykiSuoritusRepository,
                arvioijaRepository = ykiArvioijaRepository,
            )

        // Act
        ykiService.importYkiSuoritukset(null, null, false)

        // Assert
        val suoritukset = ykiSuoritusRepository.findAll()
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
                    ,"010180-9026","N","Öhmana-Testi","Ranja Testi","EST","Testikuja 5","40100","Testilä","testi@testi.fi",183424,2024-10-30T13:53:56Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        // System under test
        val ykiService =
            YkiService(
                solkiRestClient = mockRestClientBuilder.build(),
                suoritusRepository = ykiSuoritusRepository,
                arvioijaRepository = ykiArvioijaRepository,
            )

        // Act
        assertThrows<RuntimeException> {
            ykiService.importYkiSuoritukset(null, null, false)
        }
        val suoritukset = ykiSuoritusRepository.findAll()
        assertEquals(0, suoritukset.count())
    }

    @Test
    fun `import of newest suoritukset ignores duplicates`() {
        // Arrange
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("suoritukset"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.24941612410","010180-922U","N","Torvinen-Testi","Anniina Testi","LVA","Testitie 2","95700","Testimäki","torvanniina@testi.fi",183440,2024-11-14T10:17:51Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,6,6,,6,6,,,,0,0,,
                    "1.2.246.562.24.27639310186","010180-918P","N","Haverinen-Testi","Silja Testi","EST","Testikatu 17","40960","Testisuo","silja.haverinen@testi.fi",183439,2024-11-14T10:16:13Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,5,5,,,,0,0,,
                    "1.2.246.562.24.98558310636","131168-739M","M","Heponiemi","Joonas","ISL","Testikatu 32","40960","Testisuo","joonas.heponiemi@testi.fi",183441,2024-11-14T10:36:31Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,4,4,,4,4,,,,0,0,,
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        // System under test
        val ykiService =
            YkiService(
                solkiRestClient = mockRestClientBuilder.build(),
                suoritusRepository = ykiSuoritusRepository,
                arvioijaRepository = ykiArvioijaRepository,
            )

        // Act
        val from = ykiService.importYkiSuoritukset(null, null, false)

        // Assert
        val firstSuoritukset = ykiSuoritusRepository.findAll()
        assertEquals(3, firstSuoritukset.count())

        mockServer.reset()
        mockServer
            .expect(requestTo("suoritukset?m=2024-11-14T10:36:31Z"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.98558310636","131168-739M","M","Heponiemi","Joonas","ISL","Testikatu 32","40960","Testisuo","joonas.heponiemi@testi.fi",183441,2024-11-14T10:36:31Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,4,4,,4,4,,,,0,0,,
                    "1.2.246.562.24.33342764709","010866-9260","N","Sallinen-Testi","Magdalena Testi","ALA","Testitie 3","95700","Testimäki","sallinen.magdalena@testi.fi",183442,2024-11-14T10:39:48Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-11-14,5,5,,4,6,,,,0,0,,
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        // Act
        ykiService.importYkiSuoritukset(from, null, false)

        // Assert
        val suoritukset = ykiSuoritusRepository.findAll()
        assertEquals(4, suoritukset.count())
    }

    @Test
    fun `arvioijat import persists arvioijat to DB`() {
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("arvioijat"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.24941612410","010180-922U","Torvinen-Testi","Anniina Testi","anniina.testi@yki.fi","Testiosoite 7357","00100","HELSINKI",1994-08-01,2019-06-29,2024-06-29,0,0,"rus","PT+KT"
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        val ykiService =
            YkiService(
                solkiRestClient = mockRestClientBuilder.build(),
                suoritusRepository = ykiSuoritusRepository,
                arvioijaRepository = ykiArvioijaRepository,
            )

        assertDoesNotThrow {
            ykiService.importYkiArvioijat()
        }

        val imported = ykiArvioijaRepository.findAll()
        assertEquals(1, imported.count())
    }

    @Test
    fun `consecutive arvioijat import run inserts new and updated entries`() {
        val mockRestClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(mockRestClientBuilder).build()
        mockServer
            .expect(requestTo("arvioijat"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.24941612410","010180-922U","Torvinen-Testi","Anniina Testi","anniina.testi@yki.fi","Testiosoite 7357","00100","HELSINKI",1994-08-01,2019-06-29,2024-06-29,0,0,"rus","PT+KT"
                    "1.2.246.562.24.20281155246","010180-9026","Öhmana-Testi","Ranja Testi","testi@testi.fi","Testikuja 5","40100","Testilä",1994-08-01,2019-06-29,2024-06-29,0,1,"fin","YT"
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )
        mockServer
            .expect(requestTo("arvioijat"))
            .andRespond(
                withSuccess(
                    """
                    "1.2.246.562.24.24941612410","010180-922U","Torvinen-Testi","Anniina Testi","anniina.testi@yki.fi","Testiosoite 7357","00100","HELSINKI",1994-08-01,2019-06-29,2024-06-29,0,0,"rus","PT+KT"
                    "1.2.246.562.24.20281155246","010180-9026","Öhmana-Testi","Ranja Testi","testi@testi.fi","Testikuja 5","40100","Testilä",1994-08-01,2019-06-29,2024-06-29,0,0,"fin","KT+YT"
                    "1.2.246.562.24.27639310186","010180-918P","Haverinen-Testi","Silja Testi","silja.testi@yki.fi","Testausosoite 42","00100","HELSINKI",1994-08-01,2019-06-29,2024-06-29,0,1,"ita","PT+KT+YT"
                    """.trimIndent(),
                    MediaType.TEXT_PLAIN,
                ),
            )

        val ykiService =
            YkiService(
                solkiRestClient = mockRestClientBuilder.build(),
                suoritusRepository = ykiSuoritusRepository,
                arvioijaRepository = ykiArvioijaRepository,
            )

        assertDoesNotThrow {
            ykiService.importYkiArvioijat()
        }
        var arvioijat = ykiArvioijaRepository.findAll()
        assertEquals(2, arvioijat.count())
        val ranjaBeforeUpdate = arvioijat.find { it.etunimet.startsWith("Ranja") }
        assertEquals(1, ranjaBeforeUpdate?.tila)

        Thread.sleep(1000L)

        assertDoesNotThrow {
            ykiService.importYkiArvioijat()
        }

        arvioijat = ykiArvioijaRepository.findAll()

        // 3 people entries, 1 updated entry => 4 entries total
        // Note that there are rows that are duplicated in both the first and
        // the second import. Those should be imported only once.
        assertEquals(4, arvioijat.count())
        val ranjaAfterUpdate =
            arvioijat
                .filter { it.etunimet.startsWith("Ranja") }
                .maxByOrNull { it.rekisteriintuontiaika ?: OffsetDateTime.MIN }
        assertEquals(0, ranjaAfterUpdate?.tila)
    }
}
