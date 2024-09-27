package fi.oph.kitu.oppijanumero

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
    @Value("\${kitu.oppijanumero.callerid}")
    private lateinit var callerId: String

    fun authenticateToCas() {
        val grantingTicket = casService.getGrantingTicket()
        val serviceTicket = casService.getServiceTicket(grantingTicket)

        casService.sendAuthenticationRequest(serviceTicket)
    }

    fun sendRequest(requestBuilder: HttpRequest.Builder): HttpResponse<String> {
        println("Sending CAS authenticated request")
        requestBuilder
            .header("Caller-Id", callerId)
            .header("CSRF", "CSRF")
            .header("Cookie", "CSRF=CSRF")
        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        if (isLoginToCas(response)) {
            // Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
            // ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
            println("Was redirected to CAS login")
            authenticateToCas() // gets JSESSIONID Cookie and it will be used in the next request below
            return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        } else if (response.statusCode() == 401) {
            // Oppijanumerorekisteri vastaa HTTP 401 kun sessio on vanhentunut.
            // HUOM! Oppijanumerorekisteri vastaa HTTP 401 myös jos käyttöoikeudet eivät riitä.
            println("Received HTTP 401 response")
            authenticateToCas() // gets JSESSIONID Cookie and it will be used in the next request below
            return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
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
