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
    private val casRestClient: RestClient,
    @Qualifier("oppijanumeroRestClient")
    private val oppijanumeroRestClient: RestClient,
) {
    @Value("\${kitu.palvelukayttaja.username}")
    lateinit var onrUsername: String

    @Value("\${kitu.palvelukayttaja.password}")
    lateinit var onrPassword: String

    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    @WithSpan
    fun verifyServiceTicket(serviceTicket: String): TypedResult<URI, CasError> {
        val response =
            oppijanumeroRestClient
                .get()
                .uri("/j_spring_cas_security_check?ticket=$serviceTicket")
                .exchange { request, response -> response }

        return if (response?.statusCode == HttpStatus.FOUND && response.headers.location != null) {
            TypedResult.Success(response.headers.location!!)
        } else {
            TypedResult.Failure(CasError.VerifyTicketError("Received status code ${response?.statusCode}"))
        }
    }

    @WithSpan
    fun getServiceTicket(ticketGrantingTicket: String): TypedResult<String, CasError> {
        // Step 3 - Get the response
        val body = "service=$serviceUrl/j_spring_cas_security_check"
        val response =
            casRestClient
                .post()
                .uri("/v1/tickets/$ticketGrantingTicket")
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieveEntitySafely(String::class.java)

        return if (response?.statusCode == HttpStatus.OK) {
            TypedResult.Success(response.body.toString())
        } else {
            TypedResult.Failure(
                CasError.ServiceTicketError("Unable to get the service ticket (Status: ${response?.statusCode})."),
            )
        }
    }

    @WithSpan
    fun getGrantingTicket(): TypedResult<String, CasError> {
        // Step 2 - form a request
        val username = URLEncoder.encode(onrUsername, "UTF-8")
        val password = URLEncoder.encode(onrPassword, "UTF-8")
        val body = "username=$username&password=$password"

        val response =
            casRestClient
                .post()
                .uri("/v1/tickets")
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
            TypedResult.Failure(CasError.ServiceTicketError("Unable to get service ticket"))
        }
    }
}
