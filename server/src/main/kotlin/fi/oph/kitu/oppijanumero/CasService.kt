package fi.oph.kitu.oppijanumero

import fi.oph.kitu.PeerService
import fi.oph.kitu.logging.addHttpResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class CasService(
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.palvelukayttaja.username}")
    private lateinit var onrUsername: String

    @Value("\${kitu.palvelukayttaja.password}")
    private lateinit var onrPassword: String

    @Value("\${kitu.oppijanumero.casUrl}")
    private lateinit var casUrl: String

    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    fun sendAuthenticationRequest(serviceTicket: String) {
        val authRequest =
            HttpRequest
                .newBuilder(URI.create("$serviceUrl/j_spring_cas_security_check?ticket=$serviceTicket"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()
        val authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString())
        logger.atInfo().addHttpResponse(PeerService.Oppijanumero, authRequest.uri().toString(), authResponse).log()
    }

    fun getServiceTicket(ticketGrantingTicket: String): String {
        val request =
            HttpRequest
                .newBuilder(URI.create("$casUrl/v1/tickets/$ticketGrantingTicket"))
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "service=$serviceUrl/j_spring_cas_security_check",
                    ),
                ).header("Content-Type", "application/x-www-form-urlencoded")
                .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        logger.atInfo().addHttpResponse(PeerService.Cas, request.uri().toString(), response).log()

        if (response.statusCode() != 200) {
            throw CasException(response, "Ticket service did not respond with 200 status code.")
        }

        val ticket = response.body()
        return ticket
    }

    fun getGrantingTicket(): String {
        // Step 2 - form a request
        val username = URLEncoder.encode(onrUsername, "UTF-8")
        val password = URLEncoder.encode(onrPassword, "UTF-8")
        val request =
            HttpRequest
                .newBuilder(URI.create("$casUrl/v1/tickets"))
                .POST(HttpRequest.BodyPublishers.ofString("username=$username&password=$password"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

        // Step 3 - Get the response
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        logger.atInfo().addHttpResponse(PeerService.Cas, request.uri().toString(), response).log()

        val statusCode = response.statusCode()

        if (statusCode != 201) {
            throw CasException(response, "Ticket granting service did not respond with 201 status code.")
        }

        val location = response.headers().firstValue("Location").get()
        return location.substring(location.lastIndexOf("/") + 1)
    }
}
