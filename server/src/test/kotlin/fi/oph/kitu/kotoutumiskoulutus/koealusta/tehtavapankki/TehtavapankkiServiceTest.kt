package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration.Companion.TEST_BUCKET
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import software.amazon.awssdk.services.s3.S3Client
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class, LocalStackContainerConfiguration::class)
@TestPropertySource(properties = ["spring.cloud.aws.s3.enabled=true"])
class TehtavapankkiServiceTest(
    @param:Autowired private val tehtavapankkiService: TehtavapankkiService,
    @param:Autowired private val s3Client: S3Client,
) {
    @BeforeEach
    fun emptyBucket() {
        s3Client
            .listObjectsV2 { it.bucket(TEST_BUCKET) }
            .contents()
            .forEach { obj ->
                s3Client.deleteObject { it.bucket(TEST_BUCKET).key(obj.key()) }
            }
    }

    @Test
    fun `uploadTehtavapankki kirjoittaa tiedostot S3-buckettiin`() {
        val response =
            TehtavapankkiResponse(
                questionbanks =
                    listOf(
                        TehtavapankkiResponse.Questionbank(
                            courseid = 42,
                            coursename = "Suomi alkeet",
                            xml = "<questions><q id=\"1\"/></questions>",
                        ),
                    ),
            )

        tehtavapankkiService.uploadTehtavapankki(response)

        val objects = s3Client.listObjectsV2 { it.bucket(TEST_BUCKET) }
        assertEquals(1, objects.keyCount(), "Yksi tiedosto pitäisi olla ladattu")

        val key = objects.contents().single().key()
        assertTrue(
            key.startsWith("42-Suomi_alkeet/"),
            "Avaimen pitäisi olla courseid-coursename/-prefiksillä, oli: $key",
        )

        val content =
            s3Client
                .getObject { it.bucket(TEST_BUCKET).key(key) }
                .readAllBytes()
                .toString(Charsets.UTF_8)
        assertEquals("<questions><q id=\"1\"/></questions>", content)
    }

    @Test
    fun `importTehtavapankki hakee koealustasta ja kirjoittaa S3-buckettiin`() {
        val mockServer = MockRestServiceServer.bindTo(tehtavapankkiService.restClientBuilder).build()
        mockServer
            .expect(
                requestTo(
                    "https://localhost:8080/dev/koto/webservice/rest/server.php?" +
                        "wstoken=testitoken&moodlewsrestformat=json&" +
                        "wsfunction=local_completion_export_export_question_bank",
                ),
            ).andRespond(
                withSuccess(
                    """
                    {
                      "questionbanks": [
                        {
                          "courseid": 7,
                          "coursename": "Suomi 2",
                          "xml": "<questions><q id=\"a\"/></questions>"
                        },
                        {
                          "courseid": 8,
                          "coursename": "Suomi 3",
                          "xml": "<questions><q id=\"b\"/></questions>"
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        tehtavapankkiService.koealustaToken = "testitoken"
        tehtavapankkiService.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        tehtavapankkiService.importTehtavapankki()

        mockServer.verify()

        val keys =
            s3Client
                .listObjectsV2 { it.bucket(TEST_BUCKET) }
                .contents()
                .map { it.key() }

        assertEquals(2, keys.size, "Molempien tehtäväpankkien pitäisi olla ladattu")
        assertTrue(
            keys.any { it.startsWith("7-Suomi_2/") },
            "Avaimien joukosta pitäisi löytyä 7-Suomi_2/-prefiksi, oli: $keys",
        )
        assertTrue(
            keys.any { it.startsWith("8-Suomi_3/") },
            "Avaimien joukosta pitäisi löytyä 8-Suomi_3/-prefiksi, oli: $keys",
        )

        val suomi2Key = keys.single { it.startsWith("7-Suomi_2/") }
        val suomi2Content =
            s3Client
                .getObject { it.bucket(TEST_BUCKET).key(suomi2Key) }
                .readAllBytes()
                .toString(Charsets.UTF_8)
        assertEquals("<questions><q id=\"a\"/></questions>", suomi2Content)
    }
}
