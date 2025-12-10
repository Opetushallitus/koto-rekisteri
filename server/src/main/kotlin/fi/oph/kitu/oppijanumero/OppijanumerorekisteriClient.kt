package fi.oph.kitu.oppijanumero

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
import java.net.URI

@Service
class OppijanumerorekisteriClient(
    @param:Qualifier("oauth2RestClient")
    val restClient: RestClient,
    @param:Value("\${kitu.oppijanumero.service.url}")
    val serviceUrl: String,
) {
    @WithSpan
    fun <T> onrGet(
        endpoint: String,
        responseType: Class<T>,
    ) = fetch<T, EmptyRequest>(HttpMethod.GET, endpoint, responseType = responseType)

    @WithSpan
    fun <T, R : OppijanumerorekisteriRequest> onrPost(
        endpoint: String,
        body: R,
        clazz: Class<T>,
    ) = fetch<T, R>(HttpMethod.POST, endpoint, body, clazz)

    fun <T, R : OppijanumerorekisteriRequest> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        body: OppijanumerorekisteriRequest? = null,
        responseType: Class<T>,
    ): TypedResult<T, OppijanumeroException> {
        val uri = "$serviceUrl/$endpoint"

        val rawResponse =
            restClient
                .method(httpMethod)
                .uri(URI.create(uri))
                .contentType(MediaType.APPLICATION_JSON)
                .nullableBody(body)
                .retrieveEntitySafely(String::class.java)

        if (rawResponse == null) {
            throw RuntimeException("Failed to fetch data from oppijanumerorekisteri")
        }

        return if (rawResponse.statusCode == HttpStatus.NOT_FOUND) {
            TypedResult.Failure(OppijanumeroException.OppijaNotFoundException(body ?: EmptyRequest(), rawResponse))
        } else if (rawResponse.statusCode.is4xxClientError) {
            TypedResult.Failure(OppijanumeroException.BadRequest(body ?: EmptyRequest(), rawResponse))
        } else if (!rawResponse.statusCode.is2xxSuccessful) {
            TypedResult.Failure(OppijanumeroException.UnexpectedError(body ?: EmptyRequest(), rawResponse))
        } else {
            deserializeResponse(body ?: EmptyRequest(), rawResponse, responseType)
        }
    }

    /**
     * Tries to convert `HttpResponse<String>` into the given `T`.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case [OppijanumeroException.BadResponse] will be thrown.
     * Otherwise, the underlying exception will be thrown
     */
    @WithSpan
    fun <T> deserializeResponse(
        request: OppijanumerorekisteriRequest,
        response: ResponseEntity<String>,
        clazz: Class<T>,
    ): TypedResult<T, OppijanumeroException> =
        TypedResult
            .runCatching {
                defaultObjectMapper.readValue(response.body, clazz)
            }.mapFailure { decodeError ->
                TypedResult
                    .runCatching {
                        defaultObjectMapper.readValue(
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
