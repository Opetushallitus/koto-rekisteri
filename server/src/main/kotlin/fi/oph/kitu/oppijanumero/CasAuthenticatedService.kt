package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI

@Service
class CasAuthenticatedService(
    @Qualifier("oppijanumeroRestClient")
    val restClient: RestClient,
    private val casService: CasService,
) {
    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    @Value("\${kitu.oppijanumero.callerid}")
    lateinit var callerId: String

    @WithSpan("CasAuthenticatedService.fetch")
    fun <Request : Any, Response> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        body: Request?,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        val span = Span.current()
        span.setAttribute("http.request.method", httpMethod.name())
        span.setAttribute("http.request.uri", endpoint)
        span.setAttribute("http.contentType", contentType.toString())
        span.setAttribute("http.responseType", responseType.toString())
        span.setAttribute("http.body", body?.toString())

        fun retrieveEntitySafely(uri: URI) =
            restClient
                .method(httpMethod)
                .uri(uri)
                .let {
                    body?.let { b ->
                        it
                            .body(b)
                            .contentType(contentType)
                            .headers { headers ->
                                headers.set("Caller-Id", callerId)
                                headers.set("CSRF", "CSRF")
                                headers.set("Cookie", "CSRF=CSRF")
                            }
                    } ?: it
                }.retrieveEntitySafely(responseType)

        val response = retrieveEntitySafely(URI.create(endpoint))

        // Shouldn't happen, unless oppijanumero service doesn't behave as expected
        if (response == null) {
            return TypedResult.Failure(
                CasError.CasAuthServiceError("Received null ResponseEntity on the first request"),
            )
        }

        if (!requiresLogin(response)) {
            return TypedResult.Success(response)
        }

        return casService
            // authenticate to CAS
            .getGrantingTicket()
            .flatMap(casService::getServiceTicket)
            .flatMap(casService::verifyServiceTicket)
            .flatMap { newUri ->
                val response = retrieveEntitySafely(newUri)

                if (response == null) {
                    TypedResult.Failure(
                        CasError.CasAuthServiceError("Received null ResponseEntity after authentication"),
                    )
                } else {
                    TypedResult.Success(response)
                }
            }
    }

    private fun requiresLogin(response: ResponseEntity<*>): Boolean {
        // First, check if it is a login page
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
