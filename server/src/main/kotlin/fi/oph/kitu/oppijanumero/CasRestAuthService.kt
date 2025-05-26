package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.TypedResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.net.URI

@Service
class CasRestAuthService(
    @Qualifier("casRestClient")
    val restCient: RestClient,
    val objectMapper: ObjectMapper,
) {
    final inline fun <Request : Any, reified Response : Any> authenticatedPost(
        uri: URI,
        body: Request,
        accept: MediaType,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        // TODO: Stop using objectMapper.writeValueAsString
        // It's from old implement, when the http client did not support generic Body
        // Our objectMapper probably use some custom serialization rules,
        // and the restClient should take those into considerations.
        val bodyAsString = objectMapper.writeValueAsString(body)

        val resp =
            try {
                restCient
                    .post()
                    .uri(uri)
                    .body(bodyAsString)
                    .accept(accept)
                    .retrieve()
                    .toEntity<Response>()
            } catch (e: Throwable) {
                println(e)
                e.printStackTrace()
                throw e
            }

        return TypedResult.Success(resp)
    }
}
