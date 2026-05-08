package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration
import fi.oph.kitu.LocalStackContainerConfiguration.Companion.TEST_BUCKET
import fi.oph.kitu.tehtavapankki.TehtavapakettiEntity
import fi.oph.kitu.tehtavapankki.TehtavapankkiRepository
import fi.oph.kitu.util.result.TypedResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class, LocalStackContainerConfiguration::class)
@TestPropertySource(properties = ["spring.cloud.aws.s3.enabled=true"])
class TehtavapankkiIngestServiceTest(
    @param:Autowired private val ingestService: TehtavapankkiIngestService,
    @param:Autowired private val repository: TehtavapankkiRepository,
    @param:Autowired private val s3Client: S3Client,
    @param:Autowired private val jdbc: JdbcTemplate,
) {
    private val xmlBytes by lazy {
        ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml")
            .inputStream
            .use { it.readBytes() }
    }

    @BeforeEach
    fun reset() {
        s3Client
            .listObjectsV2 { it.bucket(TEST_BUCKET) }
            .contents()
            .forEach { obj -> s3Client.deleteObject { it.bucket(TEST_BUCKET).key(obj.key()) } }
        jdbc.execute("DELETE FROM tehtavapaketti")
    }

    @Test
    fun `ingest tallentaa paketin, tehtavat, vastaukset ja tiedostot`() {
        val xmlKey = "42-Suomi_alkeet/2026-01-01.xml"
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(xmlKey) },
            RequestBody.fromBytes(xmlBytes),
        )

        val result = ingestService.ingestFromS3(xmlKey)
        assertIs<TypedResult.Success<TehtavapakettiEntity, TehtavapankkiParseError>>(result)
        val paketti = result.value

        assertEquals("moodle.koealusta", paketti.lahdejarjestelma)
        assertEquals("42", paketti.lahdeId)
        assertEquals("Suomi alkeet", paketti.nimi)
        assertEquals(xmlKey, paketti.s3Avain)
        assertEquals(42, paketti.metadata.get("courseid").asInt())
        assertEquals("Suomi_alkeet", paketti.metadata.get("sanitizedCoursename").asString())

        val tehtavat = repository.findTehtavatByPakettiId(paketti.id!!)
        // Fixture has 13 questions including 2 categories — categories are dropped.
        assertEquals(11, tehtavat.size)
        assertEquals(
            mapOf(
                "multichoice" to 3,
                "shortanswer" to 2,
                "description" to 2,
                "essay" to 2,
                "cloudpoodll" to 2,
            ),
            tehtavat.groupingBy { it.tyyppi }.eachCount(),
        )
        // Source order is preserved; jarjestys is 1..N over non-category questions.
        assertEquals((1..11).toList(), tehtavat.map { it.jarjestys })

        // Fixturilla on 2 <category>-elementtiä alussa → 2 ryhmää, source-järjestyksessä.
        val ryhmat = repository.findRyhmatByPakettiId(paketti.id)
        assertEquals(2, ryhmat.size)
        assertEquals(
            listOf("\$course\$/top", "\$course\$/top/Suomen kieli A2 - esimerkkitehtävät"),
            ryhmat.map { it.nimi },
        )
        assertEquals(listOf(1, 2), ryhmat.map { it.jarjestys })

        // Kaikki tehtävät kuuluvat toiseen ryhmään, koska molemmat kategoriat
        // ovat fixturin alussa ennen muita kysymyksiä.
        val firstMultichoice = tehtavat.first { it.tyyppi == "multichoice" }
        assertEquals(
            ryhmat[1].id,
            firstMultichoice.ryhmaId,
            "Tehtävän pitäisi kuulua category-2:sta luotuun ryhmään",
        )
        assertTrue(
            tehtavat.all { it.ryhmaId == ryhmat[1].id },
            "Kaikkien tehtävien pitäisi kuulua viimeiseen ryhmään, oli: ${tehtavat.map { it.ryhmaId }.toSet()}",
        )

        val vastaukset = repository.findVastauksetByTehtavaIds(tehtavat.mapNotNull { it.id })
        // 7 multichoice answers + 4 shortanswer answers in the fixture.
        val totalVastausCount = vastaukset.values.sumOf { it.size }
        assertEquals(11, totalVastausCount)

        // The fraction-0/100/0 multichoice has answers with those exact fractions.
        val firstMcVastaukset = vastaukset[firstMultichoice.id]!!
        assertEquals(
            listOf(0.0, 100.0, 0.0),
            firstMcVastaukset.map { it.metadata.get("fraction").asDouble() },
        )

        val tiedostot = repository.findTiedostotByTehtavaIds(tehtavat.mapNotNull { it.id })
        val allFiles = tiedostot.values.flatten()
        assertTrue(allFiles.any { it.tiedostonimi.endsWith(".png") })
        assertTrue(allFiles.any { it.tiedostonimi.endsWith(".mp3") })
        // S3-avain noudattaa <basename> assets/<filename> -konventiota.
        assertTrue(
            allFiles.all { it.s3Avain.startsWith("42-Suomi_alkeet/2026-01-01 assets/") },
            "Kaikkien assettien S3-avaimien pitäisi alkaa kansiopolulla, oli: $allFiles",
        )
    }

    @Test
    fun `toinen ingest samalle XML-sisallolle on idempotentti - palauttaa olemassa olevan paketin`() {
        val xmlKey = "42-Suomi_alkeet/2026-01-01.xml"
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(xmlKey) },
            RequestBody.fromBytes(xmlBytes),
        )

        val first = (ingestService.ingestFromS3(xmlKey) as TypedResult.Success).value

        val tehtavatBefore = jdbc.queryForObject("SELECT COUNT(*) FROM tehtava", Int::class.java)
        val tiedostotBefore = jdbc.queryForObject("SELECT COUNT(*) FROM tehtava_tiedosto", Int::class.java)

        val second = (ingestService.ingestFromS3(xmlKey) as TypedResult.Success).value
        assertEquals(first.id, second.id)

        val tehtavatAfter = jdbc.queryForObject("SELECT COUNT(*) FROM tehtava", Int::class.java)
        val tiedostotAfter = jdbc.queryForObject("SELECT COUNT(*) FROM tehtava_tiedosto", Int::class.java)
        assertEquals(tehtavatBefore, tehtavatAfter)
        assertEquals(tiedostotBefore, tiedostotAfter)
    }

    @Test
    fun `eri sisaltoiselle XML lle luodaan uusi paketti versio ja vanha sailyy`() {
        val firstKey = "42-Suomi_alkeet/2026-01-01.xml"
        val secondKey = "42-Suomi_alkeet/2026-02-01.xml"
        s3Client.putObject({ it.bucket(TEST_BUCKET).key(firstKey) }, RequestBody.fromBytes(xmlBytes))
        s3Client.putObject(
            { it.bucket(TEST_BUCKET).key(secondKey) },
            RequestBody.fromString("""<?xml version="1.0"?><quiz></quiz>"""),
        )

        val firstPaketti = (ingestService.ingestFromS3(firstKey) as TypedResult.Success).value
        Thread.sleep(20)
        val secondPaketti = (ingestService.ingestFromS3(secondKey) as TypedResult.Success).value

        assertTrue(firstPaketti.id != secondPaketti.id, "Eri hashin pitäisi tuottaa eri paketti")
        val latest = repository.findLatestPakettiBySource("moodle.koealusta", "42")
        assertEquals(secondPaketti.id, latest!!.id)
        assertNotNull(repository.findPakettiById(firstPaketti.id!!), "Vanha versio pitäisi säilyä")
    }

    @Test
    fun `ingest palauttaa NotFound kun XML ta ei loydy bucketista`() {
        val result = ingestService.ingestFromS3("ei-olemassa.xml")
        assertIs<TypedResult.Failure<TehtavapakettiEntity, TehtavapankkiParseError>>(result)
        assertEquals(TehtavapankkiParseError.NotFound, result.error)
    }
}
