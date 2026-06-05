package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import arrow.core.Either
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration.Companion.TEST_BUCKET
import fi.oph.kitu.tehtavapankki.TehtavapakettiEntity
import fi.oph.kitu.tehtavapankki.TehtavapankkiRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
    @param:Autowired private val tehtavapankkiClient: TehtavapankkiClientImpl,
    @param:Autowired private val s3Client: S3Client,
    @param:Autowired private val repository: TehtavapankkiRepository,
    @param:Autowired private val jdbc: JdbcTemplate,
) {
    @BeforeEach
    fun reset() {
        s3Client
            .listObjectsV2 { it.bucket(TEST_BUCKET) }
            .contents()
            .forEach { obj ->
                s3Client.deleteObject { it.bucket(TEST_BUCKET).key(obj.key()) }
            }
        jdbc.execute("DELETE FROM tehtavapaketti")
    }

    @Test
    fun `uploadTehtavapankki kirjoittaa tiedostot S3-buckettiin filegenerated-suffiksilla`() {
        val downloads =
            listOf(
                QuestionBankDownload(
                    metadata =
                        QuestionBankMetadata(
                            courseId = 42,
                            courseName = "Suomi alkeet",
                            published = Instant.ofEpochMilli(0),
                            generated = Instant.ofEpochMilli(1733400000000),
                            version = "test",
                            language = "fin",
                            downloadUrl = "ignored",
                        ),
                    xml = StringXmlSource("<questions><q id=\"1\"/></questions>"),
                ),
            )

        tehtavapankkiService.uploadTehtavapankki(downloads)

        val objects = s3Client.listObjectsV2 { it.bucket(TEST_BUCKET) }
        assertEquals(1, objects.keyCount(), "Yksi tiedosto pitäisi olla ladattu")

        val key = objects.contents().single().key()
        assertTrue(
            key.startsWith("42-Suomi_alkeet/"),
            "Avaimen pitäisi olla courseid-coursename/-prefiksillä, oli: $key",
        )
        assertTrue(
            key.contains("-fg1733400000000-"),
            "Avaimessa pitäisi olla filegenerated epoch-ms, oli: $key",
        )

        val content =
            s3Client
                .getObject { it.bucket(TEST_BUCKET).key(key) }
                .readAllBytes()
                .toString(Charsets.UTF_8)
        assertEquals("<questions><q id=\"1\"/></questions>", content)
    }

    @Test
    fun `importTehtavapankki lataa muuttuneet ja ohittaa skipattavat`() {
        // Edellisellä ajolla on jo tuotu courseid=7 filegenerated=1733400000:lla,
        // joten se pitäisi skipata. Courseid=8:lla ei ole vastaavaa riviä, joten se
        // pitäisi ladata.
        repository.insertPaketti(
            TehtavapakettiEntity(
                lahdejarjestelma = "moodle.koealusta",
                lahdeId = "7",
                nimi = "Suomi 2",
                versioHash = "esim-hash",
                s3Avain = null,
                lahdeFilegenerated =
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(1733400000), ZoneOffset.UTC),
            ),
        )

        val mockServer = MockRestServiceServer.bindTo(tehtavapankkiClient.restClientBuilder).build()
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
                          "coursestartdate": 0,
                          "filegenerated": 1733400000,
                          "questionbankversion": "v1",
                          "language": "fin",
                          "downloadurl": "https://localhost:8080/dev/koto/pluginfile.php/7/qb.xml"
                        },
                        {
                          "courseid": 8,
                          "coursename": "Suomi 3",
                          "coursestartdate": 0,
                          "filegenerated": 1733400001,
                          "questionbankversion": "v1",
                          "language": "fin",
                          "downloadurl": "https://localhost:8080/dev/koto/pluginfile.php/8/qb.xml"
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )
        // Vain courseid=8:n latauksen pitäisi tapahtua — courseid=7 skipataan
        // ennen pluginfile.php-kutsua.
        mockServer
            .expect(requestTo("https://localhost:8080/dev/koto/pluginfile.php/8/qb.xml?token=testitoken"))
            .andRespond(
                withSuccess("<questions><q id=\"b\"/></questions>", MediaType.APPLICATION_XML),
            )

        tehtavapankkiClient.koealustaToken = "testitoken"
        tehtavapankkiClient.koealustaBaseUrl = "https://localhost:8080/dev/koto"

        tehtavapankkiService.importTehtavapankki()

        mockServer.verify()

        val keys =
            s3Client
                .listObjectsV2 { it.bucket(TEST_BUCKET) }
                .contents()
                .map { it.key() }

        assertEquals(1, keys.size, "Vain courseid=8:n pitäisi olla ladattu, oli: $keys")
        val suomi3Key = keys.single()
        assertTrue(
            suomi3Key.startsWith("8-Suomi_3/"),
            "Ladatun avaimen pitäisi alkaa courseid=8:n kansiopolulla, oli: $suomi3Key",
        )
        assertTrue(
            suomi3Key.contains("-fg1733400001-"),
            "Avaimessa pitäisi olla filegenerated epoch-ms, oli: $suomi3Key",
        )

        val suomi3Content =
            s3Client
                .getObject { it.bucket(TEST_BUCKET).key(suomi3Key) }
                .readAllBytes()
                .toString(Charsets.UTF_8)
        assertEquals("<questions><q id=\"b\"/></questions>", suomi3Content)
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

        val tehtavapaketit = tehtavapankkiService.listAllObjects()

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
        assertEquals(emptyList(), tehtavapankkiService.listAllObjects())
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

        assertIs<Either.Right<TehtavapankkiQuiz>>(result)
        assertEquals(1, result.value.questions.size)
        assertIs<CategoryQuestion>(result.value.questions.single())
    }

    @Test
    fun `fetchAndParseFromS3 palauttaa NotFound-virheen tuntemattomalle avaimelle`() {
        val result = tehtavapankkiService.fetchAndParseFromS3("ei-olemassa.xml")

        assertIs<Either.Left<TehtavapankkiParseError>>(result)
        assertEquals(TehtavapankkiParseError.NotFound, result.value)
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
                .let { (it as Either.Right).value }
                .questions
                .filterIsInstance<DescriptionQuestion>()
                .flatMap { it.questiontext?.embeddedFiles.orEmpty() } +
                TehtavapankkiXmlParser()
                    .parse(xmlBytes.inputStream())
                    .let { (it as Either.Right).value }
                    .questions
                    .filterIsInstance<MultichoiceQuestion>()
                    .flatMap { it.questiontext?.embeddedFiles.orEmpty() }
        val expectedByName = parsedFiles.associate { it.name to Base64.getMimeDecoder().decode(it.content) }
        assertTrue(expectedByName.keys.any { it.endsWith(".png") })
        assertTrue(expectedByName.keys.any { it.endsWith(".mp3") })

        val result = tehtavapankkiService.extractAndUploadAssets(xmlKey)

        assertIs<Either.Right<AssetExtractResult>>(result)
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

        assertIs<Either.Left<TehtavapankkiParseError>>(result)
        assertEquals(TehtavapankkiParseError.NotFound, result.value)
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

        assertIs<Either.Right<AssetExtractResult>>(result)
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
