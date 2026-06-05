package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import java.time.Instant

/**
 * Lähdejärjestelmäklientti, joka tuo Koealustan kysymyspankit tehtäväpankki-pipelineen.
 * Toteutus joko kutsuu Moodlen REST-rajapintaa tai (paikalliskehityksessä) palauttaa
 * staattisen mock-sisällön ilman verkkokutsua.
 *
 * Rajapinta on jaettu kahteen vaiheeseen, jotta sisällön muuttumaton tila voidaan
 * havaita ennen kallista XML-latausta: `listQuestionBanks` palauttaa pelkät metatiedot
 * yhdellä halvalla JSON-kutsulla, ja `downloadXml` lataa varsinaisen XML:n
 * vasta kun palvelukerros on todennut sisällön muuttuneeksi.
 */
interface TehtavapankkiClient {
    fun listQuestionBanks(): List<QuestionBankMetadata>

    fun downloadXml(meta: QuestionBankMetadata): XmlSource
}

data class QuestionBankMetadata(
    val courseId: Int,
    val courseName: String,
    val published: Instant,
    val generated: Instant,
    val version: String,
    val language: String,
    val downloadUrl: String,
)
