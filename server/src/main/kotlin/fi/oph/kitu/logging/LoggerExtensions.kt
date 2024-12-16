package fi.oph.kitu.logging

import fi.oph.kitu.PeerService
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes
import io.opentelemetry.semconv.UserAgentAttributes
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.dao.DuplicateKeyException
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

fun LoggingEventBuilder.addIsDuplicateKeyException(ex: Exception): LoggingEventBuilder {
    val isDuplicateKeyException = ex is DuplicateKeyException || ex.cause is DuplicateKeyException
    this.add("isDuplicateKeyException" to isDuplicateKeyException)

    if (isDuplicateKeyException) {
        val duplicateKeyException = (if (ex is DuplicateKeyException) ex else ex.cause) as DuplicateKeyException

        val table =
            duplicateKeyException.message
                ?.substringAfter("INSERT INTO \"")
                ?.substringBefore("\"")

        val constraint =
            duplicateKeyException.cause
                ?.message
                ?.substringAfter("unique constraint \"")
                ?.substringBefore("\"")

        this.add(
            "table" to table,
            "constraint" to constraint,
        )
    }

    return this
}

fun LoggingEventBuilder.add(vararg pairs: Pair<String, Any?>): LoggingEventBuilder {
    for ((key, value) in pairs) {
        addKeyValue(key, value)
    }
    return this
}

fun <T> LoggingEventBuilder.withEvent(
    operationName: String,
    f: (event: LoggingEventBuilder) -> T,
): T {
    add("operation" to operationName)
    val start = System.currentTimeMillis()

    try {
        val ret = f(this)
        add("success" to true)
        setMessage("$operationName successful")
        return ret
    } catch (ex: Exception) {
        add("success" to false)
        addIsDuplicateKeyException(ex)
        setCause(ex)
        setMessage("$operationName failed")
        throw ex
    } finally {
        val elapsed = System.currentTimeMillis() - start
        add("duration_ms" to elapsed)
        log()
    }
}
