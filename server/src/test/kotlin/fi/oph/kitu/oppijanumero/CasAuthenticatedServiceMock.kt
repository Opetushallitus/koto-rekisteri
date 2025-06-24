package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient

class CasAuthenticatedServiceMock(
    private val posts: Map<String, TypedResult<ResponseEntity<Any>, CasError>>,
) : CasAuthenticatedService {
    override var restClient: RestClient
        get() = RestClient.builder().build()
        set(value) = println("value: $value")

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
