package fi.oph.kitu.oppijanumero

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class OppijanumeroService(
    var httpClient: HttpClient,
) {
    @Value("\${kitu.oppijanumero.username}")
    private lateinit var onrUsername: String

    @Value("\${kitu.oppijanumero.password}")
    private lateinit var onrPassword: String

    @Value("\${kitu.oppijanumero.casUrl}")
    private lateinit var casUrl: String

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

        return body
    }
}
