package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.net.URI

@Service
class CasRestAuthService(
    val tracer: Tracer,
    val casService: CasRestService,
    @Qualifier("casRestClient")
    val restCient: RestClient,
    val objectMapper: ObjectMapper,
) {
    fun authenticateToCas(): TypedResult<*, CasError> =
        tracer
            .spanBuilder("CasAuthenticatedServiceImpl.authenticateToCas")
            .startSpan()
            .use {
                casService
                    .getGrantingTicket()
                    .flatMap(casService::getServiceTicket)
                    .flatMap(casService::sendAuthenticationRequest)
            }

    final inline fun <Request : Any, reified Response : Any> authenticatedPost(
        uri: URI,
        body: Request,
        accept: MediaType,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        // TODO: Stop using objectMapper.writeValueAsString
        // It's from old implement, when the http client did not support generic Body
        // Our objectMapper probably use some custom serialization rules,
        // and the restClient should take those into considerations.
        val bodyAsString = objectMapper.writeValueAsString(body)

        val response =
            restCient
                .post()
                .uri(uri)
                .body(bodyAsString)
                .accept(accept)
                .retrieve()
                .toEntity<Response>()

        if (isLoginToCas(response)) {
            println("Authenticate to CAS")
        } else if (response.statusCode == HttpStatus.UNAUTHORIZED) {
            println("Authenticate to CAS")
        }

        return TypedResult.Success(response)
    }

    /** Check if the service that was called, redirected the response into CAS */
    fun isLoginToCas(response: ResponseEntity<*>): Boolean {
        if (response.statusCode == HttpStatus.FOUND) {
            val location = response.headers.getFirst(HttpHeaders.LOCATION)
            if (location == null) {
                return false
            }

            val hasCasLogin = location.contains("/cas/login")
            return hasCasLogin
        }
        return false
    }
}
