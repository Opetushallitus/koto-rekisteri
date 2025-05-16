package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException>
}

@Service
class OppijanumeroServiceImpl(
    private val casAuthenticatedService: CasAuthenticatedService,
    val objectMapper: ObjectMapper,
    private val tracer: Tracer,
) : OppijanumeroService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> =
        tracer
            .spanBuilder("OppijanumeroServiceImpl.getOppijanumero")
            .startSpan()
            .use { span ->
                require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
                require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
                require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
                require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

                val endpoint = "$serviceUrl/yleistunniste/hae"
                val yleistunnisteHaeRequest =
                    YleistunnisteHaeRequest(oppija.etunimet, oppija.hetu, oppija.kutsumanimi, oppija.sukunimi)

                val httpRequest =
                    HttpRequest
                        .newBuilder(URI.create(endpoint))
                        .POST(
                            HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(yleistunnisteHaeRequest),
                            ),
                        ).header("Content-Type", "application/json")

                // no need to log sendRequest, because there are request and response logging inside casAuthenticatedService.
                val rawResult = casAuthenticatedService.sendRequest(httpRequest)
                if (rawResult !is TypedResult.Success) {
                    // CAS errors are not caused by the oppija data, and thus
                    // should be handling outside default error handling flow.
                    throw (rawResult as TypedResult.Failure).error
                }

                // At this point, CAS-authentication is done succesfully,
                // but we still need to check yleistunniste/hae - specific statuses
                val rawResponse = rawResult.value
                if (rawResponse.statusCode() == 404) {
                    return@use TypedResult.Failure(
                        OppijanumeroException.OppijaNotFoundException(yleistunnisteHaeRequest),
                    )
                } else if (400 <= rawResponse.statusCode() && rawResponse.statusCode() < 500) {
                    return@use TypedResult.Failure(
                        OppijanumeroException.UnexpectedError(
                            yleistunnisteHaeRequest,
                            rawResponse,
                        ),
                    )
                } else if (rawResponse.statusCode() != 200) {
                    // If test environment throws 504, it might be actually 400
                    throw OppijanumeroException.UnexpectedError(yleistunnisteHaeRequest, rawResponse)
                }

                val onrResult =
                    tryConvertToOppijanumeroResponse<YleistunnisteHaeResponse>(yleistunnisteHaeRequest, rawResponse)

                if (onrResult is TypedResult.Failure) {
                    return@use TypedResult.Failure(onrResult.error)
                }

                val body = (onrResult as TypedResult.Success).value
                span.setAttribute("response.hasOppijanumero", body.oppijanumero.isNullOrEmpty())
                span.setAttribute("response.hasOid", body.oid.isEmpty())
                span.setAttribute("response.areOppijanumeroAndOidSame", (body.oppijanumero == body.oid))

                if (body.oppijanumero.isNullOrEmpty()) {
                    return@use TypedResult.Failure(
                        OppijanumeroException.OppijaNotIdentifiedException(yleistunnisteHaeRequest),
                    )
                }

                return@use Oid
                    .parseTyped(body.oppijanumero)
                    .mapFailure {
                        OppijanumeroException.MalformedOppijanumero(
                            yleistunnisteHaeRequest,
                            body.oppijanumero,
                        )
                    }
            }

    /**
     * Tries to convert HttpResponse<String> into the given T.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case OppijanumeroException will be thrown.
     * Otherwise, the underlying exception will be thrown
     */
    final inline fun <reified T> tryConvertToOppijanumeroResponse(
        request: YleistunnisteHaeRequest,
        response: HttpResponse<String>,
    ): TypedResult<T, OppijanumeroException> =
        TypedResult
            .runCatching {
                objectMapper.readValue(response.body(), T::class.java)
            }.mapFailure {
                val error = objectMapper.readValue(response.body(), OppijanumeroServiceError::class.java)
                OppijanumeroException(
                    request,
                    "Error from oppijanumero-service: ${error.error}",
                    error,
                )
            }
}
