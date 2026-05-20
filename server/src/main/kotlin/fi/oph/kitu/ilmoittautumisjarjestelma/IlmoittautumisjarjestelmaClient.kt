package fi.oph.kitu.ilmoittautumisjarjestelma

import arrow.core.Either
import arrow.core.left
import fi.oph.kitu.restclient.retrieveEntitySafely
import fi.oph.kitu.util.defaultObjectMapper
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
    ): Either<IlmoittautumisjarjestelmaException, T>
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
    ): Either<IlmoittautumisjarjestelmaException, T> {
        val uri = URI.create("$serviceUrl/$endpoint")
        val rawResponse =
            restClient
                .method(HttpMethod.POST)
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieveEntitySafely(String::class.java)

        return if (rawResponse == null) {
            IlmoittautumisjarjestelmaException.NullResponse(body).left()
        } else if (rawResponse.statusCode.is4xxClientError) {
            IlmoittautumisjarjestelmaException.BadRequest(body, rawResponse).left()
        } else if (!rawResponse.statusCode.is2xxSuccessful) {
            IlmoittautumisjarjestelmaException.UnexpectedError(body, rawResponse).left()
        } else {
            deserializeResponse(body, rawResponse, responseType)
        }
    }

    @WithSpan
    fun <T> deserializeResponse(
        request: IlmoittautumisjarjestelmaRequest,
        response: ResponseEntity<String>,
        clazz: Class<T>,
    ): Either<IlmoittautumisjarjestelmaException, T> =
        Either
            .catch {
                defaultObjectMapper.readValue(response.body, clazz)
            }.mapLeft { _ ->
                IlmoittautumisjarjestelmaException.MalformedResponse(request, response)
            }
}
