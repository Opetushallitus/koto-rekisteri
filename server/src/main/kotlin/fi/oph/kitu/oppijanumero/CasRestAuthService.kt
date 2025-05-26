package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.TypedResult
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

        if (!requiresLogin(response)) {
            return TypedResult.Success(response)
        }

        casService
            .getGrantingTicket()
            .flatMap(casService::getServiceTicket)
            .flatMap(casService::sendAuthenticationRequest)
            .flatMap {
                restCient
                    .post()
                    .uri(uri)
                    .body(bodyAsString)
                    .accept(accept)
                    .retrieve()
                    .toEntity<Response>()
            }
    }

    /** Check if the service that was called, redirected the response into CAS */
    fun requiresLogin(response: ResponseEntity<*>): Boolean {
        // Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
        // ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
        // authentication gets JSESSIONID Cookie and it will be used in the next request below
        if (response.statusCode == HttpStatus.FOUND) {
            val location = response.headers.getFirst(HttpHeaders.LOCATION)
            if (location == null) {
                return false
            }

            val hasCasLogin = location.contains("/cas/login")
            return hasCasLogin
        }

        // Oppijanumerorekisteri vastaa HTTP 401 kun sessio on vanhentunut.
        // HUOM! Oppijanumerorekisteri vastaa HTTP 401 myös jos käyttöoikeudet eivät riitä.
        return response.statusCode == HttpStatus.UNAUTHORIZED
    }
}
