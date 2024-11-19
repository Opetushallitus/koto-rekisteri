package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest

interface OppijanumeroService {
    fun yleistunnisteHae(request: YleistunnisteHaeRequest): Pair<Int, YleistunnisteHaeResponse>
}

@Service
class OppijanumeroServiceImpl(
    private val casAuthenticatedService: CasAuthenticatedService,
    private val objectMapper: ObjectMapper,
) : OppijanumeroService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    @Value("\${kitu.oppijanumero.service.use-mock-data}")
    private var useMockData: Boolean = false

    override fun yleistunnisteHae(request: YleistunnisteHaeRequest): Pair<Int, YleistunnisteHaeResponse> {
        val data =
            if (useMockData) {
                YleistunnisteHaeRequest(
                    etunimet = "Magdalena Testi",
                    hetu = "010866-9260",
                    kutsumanimi = "Magdalena",
                    sukunimi = "Sallinen-Testi",
                )
            } else {
                request
            }

        val httpRequest =
            HttpRequest
                .newBuilder(URI.create("$serviceUrl/yleistunniste/hae"))
                .POST(toBodyPublisher(data))
                .header("Content-Type", "application/json")

        val httpResponse = casAuthenticatedService.sendRequest(httpRequest)
        val code = httpResponse.statusCode()
        val body = objectMapper.readValue(httpResponse.body(), YleistunnisteHaeResponse::class.java)

        return Pair(code, body)
    }

    private fun toBodyPublisher(request: YleistunnisteHaeRequest): HttpRequest.BodyPublisher =
        HttpRequest.BodyPublishers.ofString(
            """{
                "etunimet": "${request.etunimet}",
                "hetu": "${request.hetu}",
                "kutsumanimi": "${request.kutsumanimi}",
                "sukunimi": "${request.sukunimi}"
            }""".trim(),
        )
}
