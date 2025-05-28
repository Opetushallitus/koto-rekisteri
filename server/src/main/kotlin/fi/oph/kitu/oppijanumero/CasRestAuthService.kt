package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.TypedResult
import fi.oph.kitu.printCookies
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.CookieManager
import java.net.URI

@Service
class CasRestAuthService(
    val tracer: Tracer,
    val casService: CasRestService,
    @Qualifier("casRestClient")
    val restCient: RestClient,
    val objectMapper: ObjectMapper,
    val cookieManager: CookieManager,
) {
    final inline fun <Request : Any, reified Response : Any> authenticatedPost(
        uri: URI,
        body: Request,
        contentType: MediaType,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        // TODO: Stop using objectMapper.writeValueAsString
        // It's from old implement, when the http client did not support generic Body
        // Our objectMapper probably use some custom serialization rules,
        // and the restClient should take those into considerations.
        val bodyAsString = objectMapper.writeValueAsString(body)

        cookieManager.printCookies("authenticatedPost 1. cookieStore:\n")
        val response =
            restCient
                .post()
                .uri(uri)
                .body(bodyAsString)
                .contentType(contentType)
                .retrieveEntitySafely<Response>()
        if (response == null) {
            // TODO: Don't use ServiceTicketError
            return TypedResult.Failure(CasError.ServiceTicketError("Received null ResponseEntity on the first request"))
        }

        cookieManager.printCookies("authenticatedPost 2. cookieStore:\n")

        try {
            return if (requiresLogin(response)) {
                casService
                    .getGrantingTicket()
                    .flatMap(casService::getServiceTicket)
                    .flatMap(casService::verifyServiceTicket)
                    .flatMap {
                        println("Got response: $it")
                        val response =
                            restCient
                                .post()
                                .uri(uri)
                                .body(bodyAsString)
                                .contentType(contentType)
                                .retrieveEntitySafely<Response>()

                        if (response == null) {
                            // TODO: Don't use service ticket error
                            TypedResult.Failure(
                                CasError.ServiceTicketError("Received null ResponseEntity after authentication"),
                            )
                        } else {
                            TypedResult.Success(response)
                        }
                    }
            } else {
                TypedResult.Success(response)
            }
        } catch (e: Throwable) {
            println(e)
            e.printStackTrace()
            throw e
        }
    }

    /** Check if the service that was called, redirected the response into CAS */
    fun requiresLogin(response: ResponseEntity<*>): Boolean {
        // Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
        // ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
        // authentication gets JSESSIONID Cookie and it will be used in the next request below
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
