package fi.oph.kitu.oppijanumero

import fi.oph.kitu.PeerService
import fi.oph.kitu.logging.addResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class CasAuthenticatedService(
    @Qualifier("oppijanumeroHttpClient")
    private val httpClient: HttpClient,
    private val casService: CasService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.oppijanumero.callerid}")
    private lateinit var callerId: String

    fun authenticateToCas() {
        val grantingTicket = casService.getGrantingTicket()
        val serviceTicket = casService.getServiceTicket(grantingTicket)

        casService.sendAuthenticationRequest(serviceTicket)
    }

    fun sendRequest(requestBuilder: HttpRequest.Builder): HttpResponse<String> {
        requestBuilder
            .header("Caller-Id", callerId)
            .header("CSRF", "CSRF")
            .header("Cookie", "CSRF=CSRF")
        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        logger.atInfo().addResponse(response, PeerService.Oppijanumero).log()

        if (isLoginToCas(response)) {
            // Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
            // ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
            authenticateToCas() // gets JSESSIONID Cookie and it will be used in the next request below
            val authenticatedResponse = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            logger.atInfo().addResponse(authenticatedResponse, PeerService.Oppijanumero).log()

            return authenticatedResponse
        } else if (response.statusCode() == 401) {
            // Oppijanumerorekisteri vastaa HTTP 401 kun sessio on vanhentunut.
            // HUOM! Oppijanumerorekisteri vastaa HTTP 401 myös jos käyttöoikeudet eivät riitä.
            authenticateToCas() // gets JSESSIONID Cookie and it will be used in the next request below
            val authenticatedResponse = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            logger.atInfo().addResponse(authenticatedResponse, PeerService.Oppijanumero).log()
            return authenticatedResponse
        } else if (response.statusCode() != 200) {
            throw RuntimeException("Unexpected HTTP status code: ${response.statusCode()}")
        } else {
            return response
        }
    }

    private fun isLoginToCas(response: HttpResponse<*>): Boolean {
        if (response.statusCode() == 302) {
            val header = response.headers().firstValue("Location")
            return header.map { location: String -> location.contains("/cas/login") }.orElse(false)
        }
        return false
    }
}
