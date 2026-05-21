package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.nio.file.Path

/**
 * Paikallisen kehityksen mock-klientti: palauttaa testidatana käytetyn
 * tehtäväpankki-fikstuurin sellaisenaan, jotta koko pipeline (S3 → dedup →
 * asset-purku → tietokannan ingest → näkymä) voi toimia ilman verkkoa
 * tai oikean Moodle-tunnuksen olemassaoloa.
 *
 * Asettamalla ympäristömuuttujan `MOCK_TEHTAVAPAKETTI_XML` voi käyttää
 * mielivaltaista paikallista XML-tiedostoa fikstuurin sijaan. Kurssin
 * nimi johdetaan tällöin tiedostonimestä ja sisältö luetaan streaminä —
 * tiedosto ei missään vaiheessa materialisoidu Java-merkkijonoksi, joten
 * useamman gigatavun XML-tiedostotkin toimivat.
 */
@Service
@Profile("local-opintopolku")
class MockTehtavapankkiClient : TehtavapankkiClient {
    override fun importQuestionBanks(): TehtavapankkiResponse {
        val customXmlPath = System.getenv("MOCK_TEHTAVAPAKETTI_XML")
        val questionbank =
            if (customXmlPath.isNullOrBlank()) {
                TehtavapankkiResponse.Questionbank(
                    courseid = 42,
                    coursename = "Suomi alkeet",
                    xml = ClassPathXmlSource("kotoutumiskoulutus/tehtavapankki/tehtavapankki-fixture.xml"),
                )
            } else {
                val path = Path.of(customXmlPath)
                TehtavapankkiResponse.Questionbank(
                    courseid = customXmlPath.fold(0) { acc, c -> acc + c.code } % 10000,
                    coursename = path.fileName.toString().substringBeforeLast('.'),
                    xml = FileXmlSource(path),
                )
            }
        return TehtavapankkiResponse(questionbanks = listOf(questionbank))
    }
}
