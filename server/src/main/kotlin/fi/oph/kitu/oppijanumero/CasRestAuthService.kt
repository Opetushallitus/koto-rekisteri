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
        accept: MediaType,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        // TODO: Stop using objectMapper.writeValueAsString
        // It's from old implement, when the http client did not support generic Body
        // Our objectMapper probably use some custom serialization rules,
        // and the restClient should take those into considerations.
        val bodyAsString = objectMapper.writeValueAsString(body)

        print("authenticatedPost 1. cookieStore: ")
        cookieManager.cookieStore.cookies.forEach(::println)

        val response =
            restCient
                .post()
                .uri(uri)
                .body(bodyAsString)
                .accept(accept)
                // TODO: Use exchange instead of retrieve, because retrieve throws an exception when non 2xx status-code
                .retrieve()
                .toEntity<Response>()

        print("authenticatedPost 1. cookieStore: ")
        cookieManager.cookieStore.cookies.forEach(::println)

        try {
            return if (requiresLogin(response)) {
                casService
                    .getGrantingTicket()
                    .flatMap(casService::getServiceTicket)
                    .flatMap(casService::sendAuthenticationRequest)
                    .flatMap {
                        println("Got response: $it")
                        TypedResult.Success(
                            restCient
                                .post()
                                .uri(uri)
                                .body(bodyAsString)
                                .accept(accept)
                                .retrieve()
                                .toEntity<Response>(),
                        )
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
