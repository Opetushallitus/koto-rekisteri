package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

/**
 * Paikallisen kehityksen mock-klientti: palauttaa testidatana käytetyn
 * tehtäväpankki-fikstuurin sellaisenaan, jotta koko pipeline (S3 → dedup →
 * asset-purku → tietokannan ingest → näkymä) voi toimia ilman verkkoa
 * tai oikean Moodle-tunnuksen olemassaoloa.
 */
@Service
@Profile("local-opintopolku")
class MockTehtavapankkiClient : TehtavapankkiClient {
    private val fixtureXml: String by lazy {
        ClassPathResource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml")
            .inputStream
            .use { it.readBytes() }
            .toString(Charsets.UTF_8)
    }

    override fun importQuestionBanks(): TehtavapankkiResponse =
        TehtavapankkiResponse(
            questionbanks =
                listOf(
                    TehtavapankkiResponse.Questionbank(
                        courseid = 42,
                        coursename = "Suomi alkeet",
                        xml = fixtureXml,
                    ),
                ),
        )
}
