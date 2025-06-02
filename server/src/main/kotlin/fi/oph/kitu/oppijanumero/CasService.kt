package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.URLEncoder

@Service
class CasService(
    @Qualifier("casRestClient")
    val restClient: RestClient,
) {
    @Value("\${kitu.palvelukayttaja.username}")
    private lateinit var onrUsername: String

    @Value("\${kitu.palvelukayttaja.password}")
    private lateinit var onrPassword: String

    @Value("\${kitu.oppijanumero.url}")
    private lateinit var baseUrl: String

    @WithSpan
    fun verifyServiceTicket(
        service: String,
        ticket: String,
    ): TypedResult<URI, CasError> {
        val response =
            restClient
                .get()
                .uri("/$service/j_spring_cas_security_check?ticket=$ticket")
                .exchange { request, response -> response }

        return if (response?.statusCode == HttpStatus.FOUND && response.headers.location != null) {
            TypedResult.Success(response.headers.location!!)
        } else {
            TypedResult.Failure(CasError.VerifyTicketError("Received status code ${response?.statusCode}"))
        }
    }

    @WithSpan
    fun getServiceTicket(
        service: String,
        ticketGrantingTicket: String,
    ): TypedResult<String, CasError> {
        // Step 3 - Get the response
        val body = "service=$baseUrl$service/j_spring_cas_security_check"
        val response =
            restClient
                .post()
                .uri("cas/v1/tickets/$ticketGrantingTicket")
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieveEntitySafely(String::class.java)

        return if (response?.statusCode == HttpStatus.OK) {
            TypedResult.Success(response.body.toString())
        } else {
            TypedResult.Failure(CasError.ServiceTicketError("Unable to get service ticket"))
        }
    }

    @WithSpan
    fun getGrantingTicket(): TypedResult<String, CasError> {
        // Step 2 - form a request
        val username = URLEncoder.encode(onrUsername, "UTF-8")
        val password = URLEncoder.encode(onrPassword, "UTF-8")
        val body = "username=$username&password=$password"

        val response =
            restClient
                .post()
                .uri("cas/v1/tickets")
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieveEntitySafely(String::class.java)

        val ticket =
            response
                ?.headers
                ?.location
                ?.path
                ?.substringAfterLast("/")

        return if (response?.statusCode == HttpStatus.CREATED && ticket != null) {
            TypedResult.Success(ticket)
        } else {
            TypedResult.Failure(CasError.GrantingTicketError("Unable to get granting ticket"))
        }
    }
}
