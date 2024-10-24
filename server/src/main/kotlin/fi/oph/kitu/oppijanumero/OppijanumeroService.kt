package fi.oph.kitu.oppijanumero

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class OppijanumeroService(
    private val casAuthenticatedService: CasAuthenticatedService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.oppijanumero.serviceUrl}")
    private lateinit var serviceUrl: String

    fun yleistunnisteHae(request: YleistunnisteHaeRequest): HttpResponse<String> {
        val httpRequest =
            HttpRequest
                .newBuilder(URI.create("$serviceUrl/yleistunniste/hae"))
                .POST(toBodyPublisher(request))
                .header("Content-Type", "application/json")

        return casAuthenticatedService.sendRequest(httpRequest)
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

data class YleistunnisteHaeRequest(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
)
