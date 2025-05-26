package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.net.URLEncoder

@Service
class CasRestService(
    @Qualifier("casRestClient")
    val restClient: RestClient,
) {
    @Value("\${kitu.palvelukayttaja.username}")
    private lateinit var onrUsername: String

    @Value("\${kitu.palvelukayttaja.password}")
    private lateinit var onrPassword: String

    fun sendAuthenticationRequest() {
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
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .toEntity<String>()

        val ticket =
            response.headers.location
                ?.path
                ?.substringAfterLast("/")

        return if (response.statusCode == HttpStatus.OK && ticket != null) {
            TypedResult.Success(ticket)
        } else {
            TypedResult.Failure(CasError.GrantingTicketError("Unable to get granting ticket"))
        }
    }
}
