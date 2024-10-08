package fi.oph.kitu.oppijanumero

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
    @Value("\${kitu.oppijanumero.username}")
    private lateinit var onrUsername: String

    @Value("\${kitu.oppijanumero.password}")
    private lateinit var onrPassword: String

    @Value("\${kitu.oppijanumero.casUrl}")
    private lateinit var casUrl: String

    @Value("\${kitu.oppijanumero.serviceUrl}")
    private lateinit var serviceUrl: String

    fun sendAuthenticationRequest(serviceTicket: String) {
        val authRequest =
            HttpRequest
                .newBuilder(URI.create("$serviceUrl/j_spring_cas_security_check?ticket=$serviceTicket"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()
        val authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString())
        println("Auth reset response: $authResponse")
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

        if (response.statusCode() != 200) {
            throw RuntimeException("Unexpected status code: ${response.statusCode()} and message: ${response.body()}")
        }

        val ticket = response.body()
        println("Successfully got service ticket $ticket")
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
        val statusCode = response.statusCode()
        val body = response.body()

        if (statusCode != 201) {
            throw RuntimeException("Ticket granting service responded with status code $statusCode and message $body")
        }

        val location = response.headers().firstValue("Location").get()
        val ticket = location.substring(location.lastIndexOf("/") + 1)
        println("Successfully fetched TGT (Ticket Granting Ticket): $ticket")

        return ticket
    }
}
