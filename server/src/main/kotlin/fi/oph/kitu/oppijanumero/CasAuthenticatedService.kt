package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class CasAuthenticatedService(
    val restClientBuilder: RestClient.Builder,
    private val casService: CasService,
) {
    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    @Value("\${kitu.oppijanumero.callerid}")
    lateinit var callerId: String

    private val restClient by lazy {
        restClientBuilder
            .baseUrl(serviceUrl)
            .defaultHeaders { headers ->
                headers["Caller-Id"] = callerId
                headers["CSRF"] = "CSRF"
                headers["Cookie"] = "CSRF=CSRF"
            }.build()
    }

    @WithSpan
    fun <Request : Any, Response> post(
        endpoint: String,
        body: Request,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        println("test")
        val response =
            restClient
                .post()
                .uri(endpoint)
                .body(body)
                .contentType(contentType)
                .retrieveEntitySafely(responseType)

        // Shouldn't happen, unless pppijanumero don't behave as expected
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
            .flatMap { newURI ->
                val response =
                    restClient
                        .post()
                        .uri(newURI)
                        .body(body)
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

    private fun requiresLogin(response: ResponseEntity<*>): Boolean {
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
