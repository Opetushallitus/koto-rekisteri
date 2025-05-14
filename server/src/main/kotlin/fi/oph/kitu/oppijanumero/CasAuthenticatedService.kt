package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface CasAuthenticatedService {
    fun sendRequest(requestBuilder: HttpRequest.Builder): TypedResult<HttpResponse<String>, CasError>
}

@Service
class CasAuthenticatedServiceImpl(
    @Qualifier("oppijanumeroHttpClient")
    private val httpClient: HttpClient,
    private val casService: CasService,
    private val tracer: Tracer,
) : CasAuthenticatedService {
    @Value("\${kitu.oppijanumero.callerid}")
    private lateinit var callerId: String

    private fun authenticateToCas(): TypedResult<Unit, CasError> =
        tracer
            .spanBuilder("CasAuthenticatedServiceImpl.authenticateToCas")
            .startSpan()
            .use {
                casService
                    .getGrantingTicket()
                    .flatMap(casService::getServiceTicket)
                    .flatMap(casService::sendAuthenticationRequest)
            }

    @WithSpan
    override fun sendRequest(requestBuilder: HttpRequest.Builder): TypedResult<HttpResponse<String>, CasError> {
        requestBuilder
            .header("Caller-Id", callerId)
            .header("CSRF", "CSRF")
            .header("Cookie", "CSRF=CSRF")
        val request = requestBuilder.build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (isLoginToCas(response)) {
            // Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
            // ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
            return authenticateToCas() // gets JSESSIONID Cookie and it will be used in the next request below
                .flatMap {
                    val authenticatedRequest = requestBuilder.build()
                    val authenticatedResponse =
                        httpClient.send(
                            authenticatedRequest,
                            HttpResponse.BodyHandlers.ofString(),
                        )

                    TypedResult.Success<HttpResponse<String>, CasError>(authenticatedResponse)
                }
        } else if (response.statusCode() == 401) {
            // Oppijanumerorekisteri vastaa HTTP 401 kun sessio on vanhentunut.
            // HUOM! Oppijanumerorekisteri vastaa HTTP 401 myös jos käyttöoikeudet eivät riitä.
            return authenticateToCas() // gets JSESSIONID Cookie and it will be used in the next request below
                .flatMap {
                    val authenticatedRequest = requestBuilder.build()
                    val authenticatedResponse =
                        httpClient.send(authenticatedRequest, HttpResponse.BodyHandlers.ofString())

                    TypedResult.Success(authenticatedResponse)
                }
        }

        // loput statuskoodit oletetaan johtuvan kutsuttuvasta rajapinnasta
        return TypedResult.Success(response)
    }

    private fun isLoginToCas(response: HttpResponse<*>): Boolean {
        if (response.statusCode() == 302) {
            val header = response.headers().firstValue("Location")
            return header.map { location: String -> location.contains("/cas/login") }.orElse(false)
        }
        return false
    }
}
