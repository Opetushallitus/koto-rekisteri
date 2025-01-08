package fi.oph.kitu.logging

import fi.oph.kitu.PeerService
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes
import io.opentelemetry.semconv.UserAgentAttributes
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import java.net.http.HttpResponse

fun LoggingEventBuilder.addServletRequest(request: HttpServletRequest): LoggingEventBuilder =
    add(
        UrlAttributes.URL_PATH.toString() to request.requestURI,
        UrlAttributes.URL_QUERY.toString() to request.queryString,
        UrlAttributes.URL_SCHEME.toString() to (request.scheme ?: "http"),
        HttpAttributes.HTTP_REQUEST_METHOD.toString() to request.method,
        UserAgentAttributes.USER_AGENT_ORIGINAL.toString() to request.getHeader(HttpHeaders.USER_AGENT),
    )

fun LoggingEventBuilder.addServletResponse(response: HttpServletResponse): LoggingEventBuilder =
    add(
        HttpAttributes.HTTP_RESPONSE_STATUS_CODE.toString() to response.status,
        HttpIncubatingAttributes.HTTP_RESPONSE_BODY_SIZE.toString() to
            response.getHeader(HttpHeaders.CONTENT_LENGTH),
    )

fun LoggingEventBuilder.addHttpResponse(
    peerService: PeerService,
    uri: String,
    response: HttpResponse<*>,
): LoggingEventBuilder = addHttpResponse(peerService, uri, response.statusCode(), response.headers())

fun LoggingEventBuilder.addHttpResponse(
    peerService: PeerService,
    uri: String,
    response: ResponseEntity<*>,
): LoggingEventBuilder = addHttpResponse(peerService, uri, response.statusCode, response.headers)

fun LoggingEventBuilder.addHttpResponse(
    peerService: PeerService,
    uri: String,
    status: Any,
    headers: Any,
): LoggingEventBuilder =
    this.add(
        "peer.service" to peerService.value,
        "response.uri" to uri,
        "response.status" to status,
        "response.headers" to headers,
    )

fun LoggingEventBuilder.addCondition(
    key: String,
    condition: Boolean,
): Boolean {
    this.add(key to condition)
    return condition
}

fun LoggingEventBuilder.add(vararg pairs: Pair<String, Any?>): LoggingEventBuilder {
    for ((key, value) in pairs) {
        addKeyValue(key, value)
    }
    return this
}
