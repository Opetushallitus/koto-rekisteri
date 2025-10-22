package fi.oph.kitu.organisaatiot

import fi.oph.kitu.TypedResult
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.oppijanumero.CasAuthenticatedService
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class OrganisaatiopalveluClient(
    val casAuthenticatedService: CasAuthenticatedService,
    @param:Value("\${kitu.organisaatiopalvelu.service.url}")
    val serviceUrl: String,
) {
    @WithSpan
    fun <T> get(
        endpoint: String,
        responseType: Class<T>,
    ) = fetch<T, EmptyRequest>(HttpMethod.GET, endpoint, responseType = responseType)

    @WithSpan
    fun <T, R : OrganisaatiopalveluRequest> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        body: OrganisaatiopalveluRequest? = null,
        responseType: Class<T>,
    ): TypedResult<T, OrganisaatiopalveluException> {
        val url = "$serviceUrl/$endpoint"

        // no need to log sendRequest, because there are request and response logging inside casAuthenticatedService.
        val rawResult =
            casAuthenticatedService.fetch(httpMethod, url, body, MediaType.APPLICATION_JSON, String::class.java)
        if (rawResult !is TypedResult.Success) {
            // CAS errors are not caused by the oppija data, and thus
            // should be handling outside default error handling flow.
            throw (rawResult as TypedResult.Failure).error
        }

        // At this point, CAS-authentication is done succesfully,
        // but we still need to check endpoint specific statuses
        val rawResponse = rawResult.value
        if (rawResponse.statusCode == HttpStatus.NOT_FOUND) {
            return TypedResult.Failure(OrganisaatiopalveluException.NotFoundException(body ?: EmptyRequest()))
        } else if (rawResponse.statusCode.is4xxClientError) {
            return TypedResult.Failure(OrganisaatiopalveluException.BadRequest(body ?: EmptyRequest(), rawResponse))
        } else if (!rawResponse.statusCode.is2xxSuccessful) {
            throw OrganisaatiopalveluException.UnexpectedError(body ?: EmptyRequest(), rawResponse)
        }

        return deserializeResponse(body ?: EmptyRequest(), rawResult.value, responseType)
    }

    /**
     * Tries to convert `HttpResponse<String>` into the given `T`.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case [fi.oph.kitu.oppijanumero.OppijanumeroException.BadResponse] will be thrown.
     * Otherwise, the underlying exception will be thrown
     */
    @WithSpan
    fun <T> deserializeResponse(
        request: OrganisaatiopalveluRequest,
        response: ResponseEntity<String>,
        clazz: Class<T>,
    ): TypedResult<T, OrganisaatiopalveluException> =
        TypedResult
            .runCatching {
                defaultObjectMapper.readValue(response.body, clazz)
            }.mapFailure { decodeError ->
                TypedResult
                    .runCatching {
                        defaultObjectMapper.readValue(
                            response.body,
                            OrganisaatiopalveluError::class.java,
                        )
                    }.fold(
                        onSuccess = { orgError ->
                            OrganisaatiopalveluException.BadResponse(
                                request = request,
                                response = response,
                                organisaatiopalveluError = orgError,
                                cause = decodeError,
                            )
                        },
                        onFailure = { _ ->
                            OrganisaatiopalveluException.MalformedResponse(
                                request = request,
                                response = response,
                                cause = decodeError,
                            )
                        },
                    )
            }
}
