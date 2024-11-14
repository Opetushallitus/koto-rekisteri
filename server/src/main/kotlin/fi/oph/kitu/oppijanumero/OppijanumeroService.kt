package fi.oph.kitu.oppijanumero

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest

interface OppijanumeroService {
    fun yleistunnisteHae(request: YleistunnisteHaeRequest): Pair<Int, String>
}

@Service
class OppijanumeroServiceImpl(
    private val casAuthenticatedService: CasAuthenticatedService,
) : OppijanumeroService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.oppijanumero.serviceUrl}")
    private lateinit var serviceUrl: String

    override fun yleistunnisteHae(request: YleistunnisteHaeRequest): Pair<Int, String> {
        val httpRequest =
            HttpRequest
                .newBuilder(URI.create("$serviceUrl/yleistunniste/hae"))
                .POST(toBodyPublisher(request))
                .header("Content-Type", "application/json")

        val httpResponse = casAuthenticatedService.sendRequest(httpRequest)
        return Pair(httpResponse.statusCode(), httpResponse.body())
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
