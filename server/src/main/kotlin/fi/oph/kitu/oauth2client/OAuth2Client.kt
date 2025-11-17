package fi.oph.kitu.oauth2client

import fi.oph.kitu.nullableBody
import fi.oph.kitu.observability.use
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.semconv.HttpAttributes
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI

@Service
class OAuth2Client(
    @param:Qualifier("oauth2RestClient")
    val restClient: RestClient,
    val tracer: Tracer,
) {
    fun <Request : Any, Response> fetch(
        httpMethod: HttpMethod,
        uri: String,
        body: Request?,
        responseType: Class<Response>,
    ): ResponseEntity<String> =
        tracer
            .spanBuilder("Oauth2Client.fetch")
            .startSpan()
            .use { span ->
                span.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, httpMethod.name())
                span.setAttribute("http.request.uri", uri)
                span.setAttribute("http.contentType", MediaType.APPLICATION_JSON.toString())
                span.setAttribute("http.responseType", responseType.toString())
                span.setAttribute("http.body", body?.toString())

                val response =
                    restClient
                        .method(httpMethod)
                        .uri(URI.create(uri))
                        .attributes(clientRegistrationId("kielitutkintorekisteri-client"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .nullableBody(body)
                        .retrieveEntitySafely(String::class.java)

                if (response == null) {
                    throw RuntimeException("Failed to fetch data from $uri")
                }

                span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, response.statusCode.value())
                return@use response
            }
}
