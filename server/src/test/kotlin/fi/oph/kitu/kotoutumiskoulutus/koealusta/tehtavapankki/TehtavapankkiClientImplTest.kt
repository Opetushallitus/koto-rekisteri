package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import kotlin.test.assertFailsWith

class TehtavapankkiClientImplTest {
    @Test
    fun `importQuestionBanks heittaa kun XML-latausvastaus on virhe`() {
        val builder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(builder).build()
        mockServer
            .expect(
                requestTo(
                    "https://example.test/koto/webservice/rest/server.php?" +
                        "wstoken=testitoken&moodlewsrestformat=json&" +
                        "wsfunction=local_completion_export_export_question_bank",
                ),
            ).andRespond(
                withSuccess(
                    """
                    {
                      "questionbanks": [
                        {
                          "courseid": 9,
                          "coursename": "Suomi virheellinen",
                          "coursestartdate": 0,
                          "filegenerated": 0,
                          "questionbankversion": "v1",
                          "language": "fin",
                          "downloadurl": "https://example.test/koto/pluginfile.php/9/qb.xml"
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )
        mockServer
            .expect(requestTo("https://example.test/koto/pluginfile.php/9/qb.xml?token=testitoken"))
            .andRespond(withServerError())

        val client =
            TehtavapankkiClientImpl(builder).apply {
                koealustaToken = "testitoken"
                koealustaBaseUrl = "https://example.test/koto"
            }

        assertFailsWith<RestClientResponseException> {
            client.importQuestionBanks()
        }
    }
}
