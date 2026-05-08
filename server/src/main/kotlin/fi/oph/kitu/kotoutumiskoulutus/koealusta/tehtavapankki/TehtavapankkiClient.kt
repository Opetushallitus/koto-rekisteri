package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

/**
 * Lähdejärjestelmäklientti, joka tuo Koealustan kysymyspankit tehtäväpankki-pipelineen.
 * Toteutus joko kutsuu Moodlen REST-rajapintaa tai (paikalliskehityksessä) palauttaa
 * staattisen mock-sisällön ilman verkkokutsua.
 */
interface TehtavapankkiClient {
    fun importQuestionBanks(): TehtavapankkiResponse
}
