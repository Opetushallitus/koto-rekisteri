package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import io.opentelemetry.instrumentation.annotations.WithSpan
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
    @Value("\${kitu.palvelukayttaja.username}")
    private lateinit var onrUsername: String

    @Value("\${kitu.palvelukayttaja.password}")
    private lateinit var onrPassword: String

    @Value("\${kitu.oppijanumero.casUrl}")
    private lateinit var casUrl: String

    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    @WithSpan
    fun sendAuthenticationRequest(serviceTicket: String): TypedResult<HttpResponse<String>, CasError> {
        val authRequest =
            HttpRequest
                .newBuilder(URI.create("$serviceUrl/j_spring_cas_security_check?ticket=$serviceTicket"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()

        return TypedResult.Success(httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString()))
    }

    @WithSpan
    fun getServiceTicket(ticketGrantingTicket: String): TypedResult<String, CasError> {
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
        return if (response.statusCode() == 200) {
            TypedResult.Success(response.body())
        } else {
            TypedResult.Failure(CasError.ServiceTicketError("Unable to get service ticket"))
        }
    }

    @WithSpan
    fun getGrantingTicket(): TypedResult<String, CasError> {
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

        return if (response.statusCode() == 201) {
            TypedResult.Success(
                response.headers().firstValue("Location").get().let {
                    it.substring(it.lastIndexOf("/") + 1)
                },
            )
        } else {
            TypedResult.Failure(CasError.GrantingTicketError("Unable to get granting ticket"))
        }
    }
}
