package fi.oph.kitu.organisaatiot

import fi.oph.kitu.TypedResult
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.nullableBody
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Service
class OrganisaatiopalveluClient(
    @param:Qualifier("oauth2RestClient")
    val restClient: RestClient,
    @param:Value("\${kitu.organisaatiopalvelu.service.url}")
    val serviceUrl: String,
) {
    @WithSpan
    fun <T> get(
        endpoint: String,
        query: Map<String, Any> = emptyMap(),
        responseType: Class<T>,
    ) = fetch<T, EmptyRequest>(HttpMethod.GET, endpoint, query, responseType = responseType)

    @WithSpan
    fun <T, R : OrganisaatiopalveluRequest> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        query: Map<String, Any>,
        body: OrganisaatiopalveluRequest? = null,
        responseType: Class<T>,
    ): TypedResult<T, OrganisaatiopalveluException> {
        val uriBuilder = UriComponentsBuilder.fromUriString("$serviceUrl/$endpoint")
        query.forEach { (key, value) -> uriBuilder.queryParam(key, value) }
        val uri = uriBuilder.build().toUri()

        val rawResponse =
            restClient
                .method(httpMethod)
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .nullableBody(body)
                .retrieveEntitySafely(String::class.java)

        if (rawResponse == null) {
            throw RuntimeException("Failed to fetch data from organisaatiopalvelu")
        }

        if (rawResponse.statusCode == HttpStatus.NOT_FOUND) {
            return TypedResult.Failure(OrganisaatiopalveluException.NotFoundException(body ?: EmptyRequest()))
        } else if (rawResponse.statusCode.is4xxClientError) {
            return TypedResult.Failure(OrganisaatiopalveluException.BadRequest(body ?: EmptyRequest(), rawResponse))
        } else if (!rawResponse.statusCode.is2xxSuccessful) {
            throw OrganisaatiopalveluException.UnexpectedError(body ?: EmptyRequest(), rawResponse)
        }

        return deserializeResponse(body ?: EmptyRequest(), rawResponse, responseType)
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
