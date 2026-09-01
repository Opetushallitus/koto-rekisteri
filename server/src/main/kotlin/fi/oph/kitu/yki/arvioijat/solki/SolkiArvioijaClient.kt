package fi.oph.kitu.yki.arvioijat.solki

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.restclient.retrieveEntitySafely
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

interface SolkiArvioijaClient {
    fun put(request: SolkiArvioijaRequest): Either<SolkiArvioijaException, Unit>
}

/**
 * Kytkin on erillaan `kitu.yki.baseUrl`ista, koska osoite on asetettu joka ymparistossa (myos
 * local ja e2e dev-stubiin) eika sen olemassaolo siksi kerro, saako lahettaa.
 */
@Service
@ConditionalOnProperty("kitu.yki.arvioijat.solki.enabled", havingValue = "true")
class SolkiArvioijaClientImpl(
    @param:Qualifier("solkiRestClient")
    val restClient: RestClient,
) : SolkiArvioijaClient {
    @WithSpan
    override fun put(request: SolkiArvioijaRequest): Either<SolkiArvioijaException, Unit> {
        val response =
            restClient
                .method(HttpMethod.PUT)
                .uri("arvioijat/{oppijanumero}", request.arvioijanOppijanumero)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "${request.arvioijanOppijanumero}:${request.versio}")
                .body(request)
                .retrieveEntitySafely(String::class.java)

        return when {
            response == null -> {
                SolkiArvioijaException.NullResponse(request.arvioijanOppijanumero).left()
            }

            // Solkilla on uudempi versio: kitun rivi ei ole vanhentunut vaan Solki on jo ajan
            // tasalla, joten tama on onnistuminen eika virhe (suunnitelma §5.1).
            response.statusCode.value() == CONFLICT -> {
                Unit.right()
            }

            response.statusCode.is2xxSuccessful -> {
                Unit.right()
            }

            response.statusCode.value() == UNAUTHORIZED || response.statusCode.value() == FORBIDDEN -> {
                SolkiArvioijaException.Unauthorized(request.arvioijanOppijanumero, response).left()
            }

            response.statusCode.is4xxClientError -> {
                SolkiArvioijaException.BadRequest(request.arvioijanOppijanumero, response).left()
            }

            else -> {
                SolkiArvioijaException.UnexpectedError(request.arvioijanOppijanumero, response).left()
            }
        }
    }

    companion object {
        private const val CONFLICT = 409
        private const val UNAUTHORIZED = 401
        private const val FORBIDDEN = 403
    }
}
