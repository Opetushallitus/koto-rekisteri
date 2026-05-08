package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.util.result.TypedResult
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TehtavapankkiXmlParserTest {
    private val parser = TehtavapankkiXmlParser()

    private fun parseSuccess(xml: String): TehtavapankkiQuiz {
        val result = parser.parse(xml)
        assertTrue(result is TypedResult.Success, "Parsinnan piti onnistua, oli: $result")
        return result.value
    }

    @Test
    fun `kelpaava xml-fixturi parsiutuu kaikkiin 13 kysymykseen oikeilla tyypeillä`() {
        val stream =
            ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml").inputStream
        val result = parser.parse(stream)
        assertTrue(result is TypedResult.Success)
        val quiz = result.value

        assertEquals(13, quiz.questions.size)
        val countsByType = quiz.questions.groupingBy { it::class.simpleName!! }.eachCount()
        assertEquals(
            mapOf(
                "CategoryQuestion" to 2,
                "DescriptionQuestion" to 2,
                "MultichoiceQuestion" to 3,
                "ShortanswerQuestion" to 2,
                "EssayQuestion" to 2,
                "CloudpoodllQuestion" to 2,
            ),
            countsByType,
        )
    }

    @Test
    fun `multichoice-kysymyksen vastausvaihtoehtojen fraction-arvot parsiutuvat oikein`() {
        val stream =
            ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml").inputStream
        val quiz = (parser.parse(stream) as TypedResult.Success).value
        val firstMultichoice = quiz.questions.filterIsInstance<MultichoiceQuestion>().first()

        assertEquals(listOf(0.0, 100.0, 0.0), firstMultichoice.answers.map { it.fraction })
    }

    @Test
    fun `multichoice-kysymyksen sisaan upotettu png-tiedosto sailyy ehjana`() {
        val stream =
            ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml").inputStream
        val quiz = (parser.parse(stream) as TypedResult.Success).value

        val files =
            quiz.questions
                .filterIsInstance<MultichoiceQuestion>()
                .flatMap { it.questiontext?.embeddedFiles.orEmpty() }
        val png = files.firstOrNull { it.name.endsWith(".png") }

        assertNotNull(png, "Fixturilla pitäisi olla png-tiedosto multichoice-kysymyksessä")
        assertEquals("base64", png.encoding)
        assertEquals("/", png.path)
        assertTrue(png.content.length > 50_000, "Png-base64-sisällön pitäisi olla pitkä")
    }

    @Test
    fun `description-kysymyksen sisaan upotettu mp3-tiedosto sailyy ehjana`() {
        val stream =
            ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml").inputStream
        val quiz = (parser.parse(stream) as TypedResult.Success).value

        val mp3 =
            quiz.questions
                .filterIsInstance<DescriptionQuestion>()
                .flatMap { it.questiontext?.embeddedFiles.orEmpty() }
                .firstOrNull { it.name.endsWith(".mp3") }

        assertNotNull(mp3, "Fixturilla pitäisi olla mp3-tiedosto description-kysymyksessä")
        assertEquals("base64", mp3.encoding)
        assertTrue(mp3.content.length > 800_000, "Mp3-base64-sisällön pitäisi olla pitkä")
    }

    @Test
    fun `cloudpoodll-kysymyksen tags-lista parsiutuu kun tagit ovat olemassa`() {
        val stream =
            ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml").inputStream
        val quiz = (parser.parse(stream) as TypedResult.Success).value

        val cloudpoodlls = quiz.questions.filterIsInstance<CloudpoodllQuestion>()
        assertEquals(2, cloudpoodlls.size)
        assertTrue(cloudpoodlls.any { it.tags.isNotEmpty() })
        assertTrue(cloudpoodlls.any { it.tags.isEmpty() })
    }

    @Test
    fun `category-kysymys parsiutuu kun fixturin tyhjia kenttia tulkitaan`() {
        val xml =
            """
            <quiz>
              <question type="category">
                <category>
                  <text>${'$'}course${'$'}/top/A1</text>
                </category>
                <info format="html">
                  <text/>
                </info>
                <idnumber/>
              </question>
            </quiz>
            """.trimIndent()
        val q = parseSuccess(xml).questions.single() as CategoryQuestion

        assertEquals("category", q.type)
        assertEquals("\$course\$/top/A1", q.category?.text)
        assertEquals("html", q.info?.format)
        // Self-closing <text/> deserializes to "" (not null) — accept both as "no content".
        assertTrue(q.info?.text.isNullOrEmpty())
    }

    @Test
    fun `description-kysymys parsiutuu ilman upotettuja tiedostoja`() {
        val xml =
            """
            <quiz>
              <question type="description">
                <name><text>Otsikko</text></name>
                <questiontext format="html"><text>Sisalto</text></questiontext>
                <generalfeedback format="html"><text/></generalfeedback>
                <defaultgrade>0.0</defaultgrade>
                <penalty>0.0</penalty>
                <hidden>0</hidden>
                <idnumber/>
              </question>
            </quiz>
            """.trimIndent()
        val q = parseSuccess(xml).questions.single() as DescriptionQuestion

        assertEquals("description", q.type)
        assertEquals("Otsikko", q.name?.text)
        assertEquals("html", q.questiontext?.format)
        assertEquals("Sisalto", q.questiontext?.text)
        assertEquals(emptyList(), q.questiontext?.embeddedFiles)
        assertEquals(0.0, q.defaultgrade)
    }

    @Test
    fun `multichoice-kysymys ja kaikki vastausvaihtoehdot parsiutuvat`() {
        val xml =
            """
            <quiz>
              <question type="multichoice">
                <name><text>Kysymys</text></name>
                <questiontext format="html"><text>Mikä?</text></questiontext>
                <defaultgrade>1.0</defaultgrade>
                <penalty>0.33</penalty>
                <hidden>0</hidden>
                <single>true</single>
                <shuffleanswers>true</shuffleanswers>
                <answernumbering>abc</answernumbering>
                <answer fraction="100" format="html">
                  <text>A</text>
                  <feedback format="html"><text/></feedback>
                </answer>
                <answer fraction="0" format="html">
                  <text>B</text>
                  <feedback format="html"><text/></feedback>
                </answer>
              </question>
            </quiz>
            """.trimIndent()
        val q = parseSuccess(xml).questions.single() as MultichoiceQuestion

        assertEquals("multichoice", q.type)
        assertEquals(true, q.single)
        assertEquals("abc", q.answernumbering)
        assertEquals(2, q.answers.size)
        assertEquals(100.0, q.answers[0].fraction)
        assertEquals("A", q.answers[0].text)
        assertEquals(0.0, q.answers[1].fraction)
        assertEquals("B", q.answers[1].text)
    }

    @Test
    fun `shortanswer-kysymys ja sen synonyymit parsiutuvat`() {
        val xml =
            """
            <quiz>
              <question type="shortanswer">
                <name><text>Kysymys</text></name>
                <questiontext format="html"><text>Mikä on numero?</text></questiontext>
                <defaultgrade>1.0</defaultgrade>
                <penalty>0.33</penalty>
                <hidden>0</hidden>
                <usecase>0</usecase>
                <answer fraction="100" format="moodle_auto_format">
                  <text>020 123 456</text>
                  <feedback format="html"><text/></feedback>
                </answer>
                <answer fraction="100" format="moodle_auto_format">
                  <text>020123456</text>
                  <feedback format="html"><text/></feedback>
                </answer>
              </question>
            </quiz>
            """.trimIndent()
        val q = parseSuccess(xml).questions.single() as ShortanswerQuestion

        assertEquals("shortanswer", q.type)
        assertEquals(0, q.usecase)
        assertEquals(2, q.answers.size)
        assertEquals(setOf("020 123 456", "020123456"), q.answers.map { it.text }.toSet())
        assertTrue(q.answers.all { it.fraction == 100.0 && it.format == "moodle_auto_format" })
    }

    @Test
    fun `essay-kysymyksen tyhjat self-closing -kentat tulkitaan tyhjina merkkijonoina`() {
        val xml =
            """
            <quiz>
              <question type="essay">
                <name><text>Kirjoitustehtava</text></name>
                <questiontext format="html"><text>Kirjoita.</text></questiontext>
                <defaultgrade>100.0</defaultgrade>
                <penalty>0.0</penalty>
                <hidden>0</hidden>
                <responseformat>editor</responseformat>
                <responserequired>1</responserequired>
                <responsefieldlines>10</responsefieldlines>
                <minwordlimit/>
                <maxwordlimit/>
                <attachments>0</attachments>
                <attachmentsrequired>0</attachmentsrequired>
                <maxbytes>0</maxbytes>
                <filetypeslist/>
                <graderinfo format="html"><text>Arviointiohje</text></graderinfo>
                <responsetemplate format="html"><text/></responsetemplate>
              </question>
            </quiz>
            """.trimIndent()
        val q = parseSuccess(xml).questions.single() as EssayQuestion

        assertEquals("essay", q.type)
        assertEquals("editor", q.responseformat)
        assertEquals(10, q.responsefieldlines)
        assertEquals("", q.minwordlimit)
        assertEquals("", q.maxwordlimit)
        assertEquals("", q.filetypeslist)
        assertEquals("Arviointiohje", q.graderinfo?.text)
    }

    @Test
    fun `cloudpoodll-kysymys ja tagit parsiutuvat`() {
        val xml =
            """
            <quiz>
              <question type="cloudpoodll">
                <name><text>Puhe</text></name>
                <questiontext format="html"><text>Puhu.</text></questiontext>
                <defaultgrade>1.0</defaultgrade>
                <penalty>0.0</penalty>
                <hidden>0</hidden>
                <responseformat>audio</responseformat>
                <language>fi-FI</language>
                <expiredays>365</expiredays>
                <transcriber>1</transcriber>
                <studentplayer>1</studentplayer>
                <teacherplayer>1</teacherplayer>
                <transcode>1</transcode>
                <audioskin>once</audioskin>
                <videoskin>onetwothree</videoskin>
                <timelimit>240</timelimit>
                <safesave>1</safesave>
                <noaudiofilters>0</noaudiofilters>
                <tags>
                  <tag><text>puhuminen</text></tag>
                  <tag><text>A1</text></tag>
                </tags>
              </question>
            </quiz>
            """.trimIndent()
        val q = parseSuccess(xml).questions.single() as CloudpoodllQuestion

        assertEquals("cloudpoodll", q.type)
        assertEquals("fi-FI", q.language)
        assertEquals(240, q.timelimit)
        assertEquals(listOf("puhuminen", "A1"), q.tags.map { it.text })
    }

    @Test
    fun `viallinen xml palauttaa InvalidXml-virheen`() {
        val result = parser.parse("<quiz><question type=\"category\"></quiz>")
        assertTrue(result is TypedResult.Failure)
        assertTrue(result.error is TehtavapankkiParseError.InvalidXml)
    }
}
