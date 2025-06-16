package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

class CasAuthenticatedServiceMock(
    private val posts: Map<String, TypedResult<ResponseEntity<Any>, CasError>>,
) : CasAuthenticatedService {
    companion object {
        fun <Request> toKey(
            endpoint: String,
            body: Request,
            contentType: MediaType,
            responseType: Class<*>,
        ): String =
            "{" +
                """ "endpoint":"$endpoint",""" +
                """ "body":"$body",""" +
                """ "contentType":"$contentType", """ +
                """ "responseType":"$responseType" """ +
                "}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Request : Any, Response> post(
        endpoint: String,
        body: Request,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError> =
        posts[toKey(endpoint, body, contentType, responseType)] as TypedResult<ResponseEntity<Response>, CasError>
}
