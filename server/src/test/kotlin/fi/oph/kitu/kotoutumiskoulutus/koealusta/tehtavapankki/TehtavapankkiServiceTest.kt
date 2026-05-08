package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration.Companion.TEST_BUCKET
import fi.oph.kitu.util.result.TypedResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun `listTehtavapaketit palauttaa bucketin objektit`() {
        val startTime = Instant.now()
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("42-Suomi_alkeet/2026-01-01T00:00:00-0.xml") },
            RequestBody.fromString("<questions/>"),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("7-Suomi_2/2026-02-02T00:00:00-0.xml") },
            RequestBody.fromString("<questions/>"),
        )
        val endTime = Instant.now()

        val tehtavapaketit = tehtavapankkiService.listAllTehtavapaketit()

        assertEquals(2, tehtavapaketit.size, "Molempien objektien pitäisi näkyä listauksessa")
        assertEquals(
            setOf(
                "42-Suomi_alkeet/2026-01-01T00:00:00-0.xml",
                "7-Suomi_2/2026-02-02T00:00:00-0.xml",
            ),
            tehtavapaketit.map { it.key }.toSet(),
        )
        tehtavapaketit.forEach { tp ->
            assertTrue(
                !tp.timestamp.isBefore(startTime.truncatedTo(ChronoUnit.SECONDS)) &&
                    !tp.timestamp.isAfter(endTime.plusSeconds(1)),
                "Aikaleiman ${tp.timestamp} pitäisi olla välillä $startTime..$endTime, avain: ${tp.key}",
            )
        }
    }

    @Test
    fun `listTehtavapaketit palauttaa tyhjan listan kun bucketissa ei ole objekteja`() {
        assertEquals(emptyList(), tehtavapankkiService.listAllTehtavapaketit())
    }

    @Test
    fun `getTemporaryDownloadUrl palauttaa toimivan signed URL_n`() {
        val key = "42-Suomi_alkeet/2026-01-01T00:00:00-0.xml"
        val content = "<questions><q id=\"1\"/></questions>"
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(key) },
            RequestBody.fromString(content),
        )

        val url = tehtavapankkiService.getTemporaryDownloadUrl(key)

        assertNotNull(url, "Signed URL:n pitäisi palautua kun bucket on määritelty")
        val downloaded =
            URI
                .create(url.toString())
                .toURL()
                .openStream()
                .use { it.readBytes().toString(Charsets.UTF_8) }
        assertEquals(content, downloaded)
    }

    @Test
    fun `getTemporaryDownloadUrl palauttaa null, jos objektiavain on tuntematon`() {
        val url = tehtavapankkiService.getTemporaryDownloadUrl("42-Suomi_alkeet/2026-01-01T00:00:00-0.xml")
        assertNull(url, "Signed URL:n pitäisi olla null, kun objektia ei löydy")
    }

    @Test
    fun `fetchAndParseFromS3 lataa ja parsii bucketissa olevan xml-tiedoston`() {
        val key = "42-Suomi_alkeet/2026-01-01T00:00:00-0.xml"
        val xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <quiz>
              <question type="category">
                <category><text>${'$'}course${'$'}/top/A1</text></category>
                <info format="html"><text/></info>
                <idnumber/>
              </question>
            </quiz>
            """.trimIndent()
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(key) },
            RequestBody.fromString(xml),
        )

        val result = tehtavapankkiService.fetchAndParseFromS3(key)

        assertIs<TypedResult.Success<TehtavapankkiQuiz, TehtavapankkiParseError>>(result)
        assertEquals(1, result.value.questions.size)
        assertIs<CategoryQuestion>(result.value.questions.single())
    }

    @Test
    fun `fetchAndParseFromS3 palauttaa NotFound-virheen tuntemattomalle avaimelle`() {
        val result = tehtavapankkiService.fetchAndParseFromS3("ei-olemassa.xml")

        assertIs<TypedResult.Failure<TehtavapankkiQuiz, TehtavapankkiParseError>>(result)
        assertEquals(TehtavapankkiParseError.NotFound, result.error)
    }

    @Test
    fun `removeDuplicates poistaa kustakin kansiosta saman sisallon duplikaatit ja jattaa vanhimman`() {
        val sameContent = "<quiz><q id=\"1\"/></quiz>"
        val otherContent = "<quiz><q id=\"2\"/></quiz>"

        // 42-Suomi_alkeet/: 3 duplikaattia + 1 erilainen → poistettava 2
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("42-Suomi_alkeet/2026-01-01T00-00-00-0.xml") },
            RequestBody.fromString(sameContent),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("42-Suomi_alkeet/2026-02-01T00-00-00-0.xml") },
            RequestBody.fromString(sameContent),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("42-Suomi_alkeet/2026-03-01T00-00-00-0.xml") },
            RequestBody.fromString(sameContent),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("42-Suomi_alkeet/2026-04-01T00-00-00-0.xml") },
            RequestBody.fromString(otherContent),
        )

        // 7-Suomi_2/: 2 saman sisältöistä → poistettava 1
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("7-Suomi_2/2026-01-01T00-00-00-0.xml") },
            RequestBody.fromString(sameContent),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("7-Suomi_2/2026-02-01T00-00-00-0.xml") },
            RequestBody.fromString(sameContent),
        )

        val result = tehtavapankkiService.removeDuplicates()

        assertNotNull(result)
        assertEquals(6, result.scanned)
        assertEquals(3, result.deleted.size)

        val remaining =
            s3Client
                .listObjectsV2 { it.bucket(TEST_BUCKET) }
                .contents()
                .map { it.key() }
        assertEquals(3, remaining.size)
        // 42-Suomi_alkeet/: yksi sameContent + yksi otherContent
        val alkeetKeys = remaining.filter { it.startsWith("42-Suomi_alkeet/") }
        assertEquals(2, alkeetKeys.size)
        assertTrue(alkeetKeys.any { it.endsWith("2026-04-01T00-00-00-0.xml") })
        // 7-Suomi_2/: tasan yksi tiedosto
        assertEquals(1, remaining.count { it.startsWith("7-Suomi_2/") })
    }

    @Test
    fun `removeDuplicates ei poista mitaan kun bucket on tyhja`() {
        val result = tehtavapankkiService.removeDuplicates()

        assertNotNull(result)
        assertEquals(0, result.scanned)
        assertEquals(emptyList(), result.deleted)
    }

    @Test
    fun `extractAndUploadAssets purkaa upotetut tiedostot omiksi S3-objekteiksi`() {
        val xmlBytes =
            ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml")
                .inputStream
                .use { it.readBytes() }
        val xmlKey = "42-Suomi/2026-01-01.xml"
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(xmlKey) },
            RequestBody.fromBytes(xmlBytes),
        )

        // Lasketaan odotettu sisältö parsimalla XML samalla parserilla.
        val parsedFiles =
            TehtavapankkiXmlParser()
                .parse(xmlBytes.inputStream())
                .let { (it as TypedResult.Success).value }
                .questions
                .filterIsInstance<DescriptionQuestion>()
                .flatMap { it.questiontext?.embeddedFiles.orEmpty() } +
                TehtavapankkiXmlParser()
                    .parse(xmlBytes.inputStream())
                    .let { (it as TypedResult.Success).value }
                    .questions
                    .filterIsInstance<MultichoiceQuestion>()
                    .flatMap { it.questiontext?.embeddedFiles.orEmpty() }
        val expectedByName = parsedFiles.associate { it.name to Base64.getMimeDecoder().decode(it.content) }
        assertTrue(expectedByName.keys.any { it.endsWith(".png") })
        assertTrue(expectedByName.keys.any { it.endsWith(".mp3") })

        val result = tehtavapankkiService.extractAndUploadAssets(xmlKey)

        assertIs<TypedResult.Success<AssetExtractResult, TehtavapankkiParseError>>(result)
        assertEquals(emptyList(), result.value.failed)
        assertEquals(expectedByName.size, result.value.uploadedAssets.size)

        val expectedKeys = expectedByName.keys.map { "42-Suomi/2026-01-01 assets/$it" }.toSet()
        assertEquals(expectedKeys, result.value.uploadedAssets.toSet())

        // Verify a couple of the assets actually contain the decoded bytes.
        expectedByName.forEach { (name, expectedBytes) ->
            val downloaded =
                s3Client
                    .getObject { it.bucket(TEST_BUCKET).key("42-Suomi/2026-01-01 assets/$name") }
                    .readAllBytes()
            assertEquals(
                expectedBytes.size,
                downloaded.size,
                "Asset $name byte length mismatch",
            )
            assertTrue(expectedBytes.contentEquals(downloaded), "Asset $name content mismatch")
        }
    }

    @Test
    fun `extractAndUploadAssets palauttaa NotFound-virheen tuntemattomalle xml-avaimelle`() {
        val result = tehtavapankkiService.extractAndUploadAssets("ei-olemassa.xml")

        assertIs<TypedResult.Failure<AssetExtractResult, TehtavapankkiParseError>>(result)
        assertEquals(TehtavapankkiParseError.NotFound, result.error)
    }

    @Test
    fun `extractAndUploadAssets ylikirjoittaa olemassaolevan asset-objektin`() {
        val xmlBytes =
            ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml")
                .inputStream
                .use { it.readBytes() }
        val xmlKey = "42-Suomi/2026-01-01.xml"
        val pngAssetKey = "42-Suomi/2026-01-01 assets/image.png"
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(xmlKey) },
            RequestBody.fromBytes(xmlBytes),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(pngAssetKey) },
            RequestBody.fromString("vanhaa-roskaa"),
        )

        val result = tehtavapankkiService.extractAndUploadAssets(xmlKey)

        assertIs<TypedResult.Success<AssetExtractResult, TehtavapankkiParseError>>(result)
        val downloaded =
            s3Client
                .getObject { it.bucket(TEST_BUCKET).key(pngAssetKey) }
                .readAllBytes()
        assertTrue(
            downloaded.size > 1000,
            "Asset olisi pitänyt ylikirjoittaa oikealla png-sisällöllä, oli ${downloaded.size} tavua",
        )
        assertTrue(
            !"vanhaa-roskaa".toByteArray().contentEquals(downloaded),
            "Roskasisällön ei pitäisi enää olla S3:ssa",
        )
    }

    @Test
    fun `removeDuplicates ei vertaa eri kansioiden valilla`() {
        val sameContent = "<quiz><q id=\"1\"/></quiz>"

        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("42-Suomi_alkeet/2026-01-01T00-00-00-0.xml") },
            RequestBody.fromString(sameContent),
        )
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key("7-Suomi_2/2026-01-01T00-00-00-0.xml") },
            RequestBody.fromString(sameContent),
        )

        val result = tehtavapankkiService.removeDuplicates()

        assertNotNull(result)
        assertEquals(2, result.scanned)
        assertEquals(emptyList(), result.deleted, "Eri kansioiden samansisältöisiä ei pitäisi poistaa")
    }
}
