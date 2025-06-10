package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException>
}

@Service
class OppijanumeroServiceImpl(
    private val casAuthenticatedService: CasAuthenticatedService,
    val objectMapper: ObjectMapper,
    private val tracer: Tracer,
) : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> =
        tracer
            .spanBuilder("OppijanumeroServiceImpl.getOppijanumero")
            .startSpan()
            .use { span ->
                require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
                require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
                require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
                require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

                val yleistunnisteHaeRequest =
                    YleistunnisteHaeRequest(oppija.etunimet, oppija.hetu, oppija.kutsumanimi, oppija.sukunimi)

                // no need to log sendRequest, because there are request and response logging inside casAuthenticatedService.
                val rawResult =
                    casAuthenticatedService.post(
                        "/yleistunniste/hae",
                        yleistunnisteHaeRequest,
                        MediaType.APPLICATION_JSON,
                        String::class.java,
                    )
                if (rawResult !is TypedResult.Success) {
                    // CAS errors are not caused by the oppija data, and thus
                    // should be handling outside default error handling flow.
                    throw (rawResult as TypedResult.Failure).error
                }

                // At this point, CAS-authentication is done succesfully,
                // but we still need to check yleistunniste/hae - specific statuses
                val rawResponse = rawResult.value
                if (rawResponse.statusCode == HttpStatus.NOT_FOUND) {
                    return@use TypedResult.Failure(
                        OppijanumeroException.OppijaNotFoundException(yleistunnisteHaeRequest),
                    )
                } else if (rawResponse.statusCode.is4xxClientError) {
                    return@use TypedResult.Failure(
                        OppijanumeroException.BadRequest(
                            yleistunnisteHaeRequest,
                            rawResponse,
                        ),
                    )
                } else if (!rawResponse.statusCode.is2xxSuccessful) {
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
     * Tries to convert `HttpResponse<String>` into the given `T`.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case [OppijanumeroException.BadResponse] will be thrown.
     * Otherwise, the underlying exception will be thrown
     */
    final inline fun <reified T> tryConvertToOppijanumeroResponse(
        request: YleistunnisteHaeRequest,
        response: ResponseEntity<String>,
    ): TypedResult<T, OppijanumeroException> =
        TypedResult
            .runCatching {
                objectMapper.readValue(response.body, T::class.java)
            }.mapFailure { decodeError ->
                TypedResult
                    .runCatching {
                        objectMapper.readValue(
                            response.body,
                            OppijanumeroServiceError::class.java,
                        )
                    }.fold(
                        onSuccess = { onrError ->
                            OppijanumeroException.BadResponse(
                                request = request,
                                response = response,
                                oppijanumeroServiceError = onrError,
                                cause = decodeError,
                            )
                        },
                        onFailure = { _ ->
                            OppijanumeroException.MalformedResponse(
                                request = request,
                                response = response,
                                cause = decodeError,
                            )
                        },
                    )
            }
}
