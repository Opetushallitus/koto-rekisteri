package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.TypedResult
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

interface CasAuthenticatedService {
    fun <Request : Any, Response : Any> authenticatedPost(
        service: String,
        endpoint: String,
        body: Request,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError>
}

@Service
class CasAuthenticatedServiceImpl(
    val tracer: Tracer,
    val casService: CasService,
    @Qualifier("casRestClient")
    val restCient: RestClient,
    val objectMapper: ObjectMapper,
) : CasAuthenticatedService {
    override fun <Request : Any, Response : Any> authenticatedPost(
        service: String,
        endpoint: String,
        body: Request,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        // TODO: Stop using objectMapper.writeValueAsString
        // It's from old implement, when the http client did not support generic Body
        // Our objectMapper probably use some custom serialization rules,
        // and the restClient should take those into considerations.
        val bodyAsString = objectMapper.writeValueAsString(body)
        val response =
            restCient
                .post()
                .uri("$service/$endpoint")
                .body(bodyAsString)
                .contentType(contentType)
                .retrieveEntitySafely(responseType)
        if (response == null) {
            return TypedResult.Failure(
                CasError.CasAuthServiceError("Received null ResponseEntity on the first request"),
            )
        }

        return if (!requiresLogin(response)) {
            TypedResult.Success(response)
        } else {
            casService
                .getGrantingTicket()
                .flatMap { ticket -> casService.getServiceTicket(service, ticket) }
                .flatMap { ticket -> casService.verifyServiceTicket(service, ticket) }
                .flatMap { newUri ->
                    val response =
                        restCient
                            .post()
                            .uri(newUri)
                            .body(bodyAsString)
                            .contentType(contentType)
                            .retrieveEntitySafely(responseType)

                    if (response == null) {
                        TypedResult.Failure(
                            CasError.CasAuthServiceError("Received null ResponseEntity after authentication"),
                        )
                    } else {
                        TypedResult.Success(response)
                    }
                }
        }
    }

    /** Check if the service that was called, redirected the response into CAS */
    fun requiresLogin(response: ResponseEntity<*>): Boolean {
        // Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
        // ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
        if (response.statusCode == HttpStatus.FOUND) {
            return response.headers
                .getFirst(HttpHeaders.LOCATION)
                ?.contains("/cas/login")
                ?: false
        }

        // Oppijanumerorekisteri vastaa HTTP 401 kun sessio on vanhentunut.
        // HUOM! Oppijanumerorekisteri vastaa HTTP 401 myös jos käyttöoikeudet eivät riitä.
        return response.statusCode == HttpStatus.UNAUTHORIZED
    }
}
