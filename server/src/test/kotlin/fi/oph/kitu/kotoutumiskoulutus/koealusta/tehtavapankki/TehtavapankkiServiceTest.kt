package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration.Companion.TEST_BUCKET
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
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
}
