package fi.oph.kitu.ilmoittautumisjarjestelma

import fi.oph.kitu.TypedResult
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI

interface IlmoittautumisjarjestelmaClient {
    fun <T> post(
        endpoint: String,
        body: IlmoittautumisjarjestelmaRequest,
        responseType: Class<T>,
    ): TypedResult<T, out IlmoittautumisjarjestelmaException>
}

@Service
@ConditionalOnProperty("kitu.ilmoittautumispalvelu.service.url")
class IlmoittautumisjarjestelmaClientImpl(
    @param:Qualifier("oauth2RestClient")
    val restClient: RestClient,
    @param:Value("\${kitu.ilmoittautumispalvelu.service.url}")
    val serviceUrl: String,
) : IlmoittautumisjarjestelmaClient {
    @WithSpan
    override fun <T> post(
        endpoint: String,
        body: IlmoittautumisjarjestelmaRequest,
        responseType: Class<T>,
    ): TypedResult<T, out IlmoittautumisjarjestelmaException> {
        val uri = URI.create("$serviceUrl/$endpoint")
        val rawResponse =
            restClient
                .method(HttpMethod.POST)
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieveEntitySafely(String::class.java)

        if (rawResponse == null) {
            throw RuntimeException("Failed to send data to kielitutkintojen ilmoittautumisjärjestelmä")
        }

        return if (rawResponse.statusCode.is4xxClientError) {
            TypedResult.Failure(IlmoittautumisjarjestelmaException.BadRequest(body, rawResponse))
        } else if (!rawResponse.statusCode.is2xxSuccessful) {
            TypedResult.Failure(IlmoittautumisjarjestelmaException.UnexpectedError(body, rawResponse))
        } else {
            deserializeResponse(body, rawResponse, responseType)
        }
    }

    @WithSpan
    fun <T> deserializeResponse(
        request: IlmoittautumisjarjestelmaRequest,
        response: ResponseEntity<String>,
        clazz: Class<T>,
    ): TypedResult<T, IlmoittautumisjarjestelmaException> =
        TypedResult
            .runCatching {
                defaultObjectMapper.readValue(response.body, clazz)
            }.mapFailure { decodeError ->
                IlmoittautumisjarjestelmaException.MalformedResponse(request, response)
            }
}
