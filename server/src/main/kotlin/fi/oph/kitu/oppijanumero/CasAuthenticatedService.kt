package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.use
import fi.oph.kitu.nullableBody
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI

interface CasAuthenticatedService {
    fun <Request : Any, Response> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        body: Request?,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError>
}

@Service
class CasAuthenticatedServiceImpl(
    @Qualifier("oppijanumeroRestClient")
    val restClient: RestClient,
    private val casService: CasService,
    private val tracer: Tracer,
) : CasAuthenticatedService {
    override fun <Request : Any, Response> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        body: Request?,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError> =
        tracer
            .spanBuilder("CasAuthenticatedService.fetch")
            .startSpan()
            .use { span ->
                span.setAttribute("http.request.method", httpMethod.name())
                span.setAttribute("http.request.uri", endpoint)
                span.setAttribute("http.contentType", contentType.toString())
                span.setAttribute("http.responseType", responseType.toString())
                span.setAttribute("http.body", body?.toString())

                val response =
                    restClient
                        .method(httpMethod)
                        .uri(URI.create(endpoint))
                        .contentType(contentType)
                        .nullableBody(body)
                        .retrieveEntitySafely(responseType)

                // Shouldn't happen, unless oppijanumero service doesn't behave as expected
                if (response == null) {
                    return TypedResult.Failure(
                        CasError.CasAuthServiceError("Received null ResponseEntity on the first request"),
                    )
                }

                if (!requiresLogin(response).also { span.setAttribute("requiresLogin", it) }) {
                    return TypedResult.Success(response)
                }

                return casService
                    // authenticate to CAS
                    .getGrantingTicket()
                    .flatMap(casService::getServiceTicket)
                    .flatMap(casService::verifyServiceTicket)
                    .flatMap { newUri ->
                        val response =
                            restClient
                                .method(httpMethod)
                                .uri(newUri)
                                .contentType(contentType)
                                .nullableBody(body)
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
