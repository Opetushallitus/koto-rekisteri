package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

class CasAuthenticatedServiceMock(
    private val posts: Map<String, TypedResult<ResponseEntity<Any>, CasError>>,
) : CasAuthenticatedService {
    companion object {
        fun <Request> toKey(
            httpMethod: HttpMethod,
            endpoint: String,
            body: Request,
            contentType: MediaType,
            responseType: Class<*>,
        ): String =
            "{" +
                """"httpMethod":"$httpMethod",""" +
                """"endpoint":"$endpoint",""" +
                """"body":"$body",""" +
                """"contentType":"$contentType",""" +
                """"responseType":"$responseType" """ +
                "}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Request : Any, Response> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        body: Request?,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError> {
        val key = toKey(httpMethod, endpoint, body, contentType, responseType)
        val result = posts[key] ?: throw AssertionError("Could not find response with key $key")

        return result as TypedResult<ResponseEntity<Response>, CasError>
    }
}
