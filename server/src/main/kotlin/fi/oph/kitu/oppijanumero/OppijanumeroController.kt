package fi.oph.kitu.oppijanumero

import fi.oph.kitu.generated.api.OppijanumeroControllerApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.net.CookieManager
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Map

@RestController
class OppijanumeroController : OppijanumeroControllerApi {
    @Value("\${kitu.oppijanumero.username}")
    lateinit var username: String

    @Value("\${kitu.oppijanumero.password}")
    lateinit var password: String

    override fun getOppijanumero(): ResponseEntity<String> {
        // Step 1 - init a client
        val httpClient =
            HttpClient
                .newBuilder()
                .cookieHandler(CookieManager())
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build()
        try {
            // Step 2 - form a request
            val baseUrl = "https://virkailija.testiopintopolku.fi/cas"
            val request =
                HttpRequest
                    .newBuilder(URI.create(baseUrl + "/v1/tickets"))
                    .POST(formBody(Map.of<String, String>("username", username, "password", password)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build()

            // Step 3 - Get the response
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val statusCode = response.statusCode()
            val body = response.body()

            return ResponseEntity(
                "Status: $statusCode, body: $body",
                HttpStatus.OK,
            )
        } catch (e: Exception) {
            println("ERROR: ${e.message}")

            return ResponseEntity(
                "An unexpected error has occurred:${e.localizedMessage}",
                HttpStatus.INTERNAL_SERVER_ERROR,
            )
        } finally {
            httpClient.close()
        }
    }

    private fun formBody(params: kotlin.collections.Map<String, String>): BodyPublisher {
        val body = StringBuilder()
        for ((key, value) in params) {
            if (body.length > 0) body.append("&")
            body.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
            body.append("=")
            body.append(URLEncoder.encode(value, StandardCharsets.UTF_8))
        }
        return HttpRequest.BodyPublishers.ofString(body.toString())
    }
}
