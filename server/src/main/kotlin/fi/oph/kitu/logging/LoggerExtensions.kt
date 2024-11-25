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

inline fun <reified T> LoggingEventBuilder.addResponse(
    response: HttpResponse<T>,
    peerService: PeerService,
): LoggingEventBuilder =
    this
        .addKeyValue("response.status", response.statusCode())
        .addKeyValue("response.headers", response.headers().toString())
        .addKeyValue("response.body", response.body().toString())
        .addKeyValue("peer.service", peerService.value)

inline fun <reified T> LoggingEventBuilder.addResponse(
    response: ResponseEntity<T>,
    peerService: PeerService,
): LoggingEventBuilder =
    this
        .addKeyValue("response.status", response.statusCode)
        .addKeyValue("response.headers", response.headers)
        .addKeyValue("response.body", response.body)
        .addKeyValue("peer.service", peerService.value)

inline fun <reified T> LoggingEventBuilder.addResponse(
    endpoint: String,
    response: ResponseEntity<T>,
): LoggingEventBuilder =
    this
        .addKeyValue("$endpoint.response.status", response.statusCode)
        .addKeyValue("$endpoint.response.headers", response.headers)
        .addKeyValue("$endpoint.response.body", response.body)

fun LoggingEventBuilder.addIsDuplicateKeyException(ex: Exception): LoggingEventBuilder {
    val isDuplicateKeyException = ex is DuplicateKeyException || ex.cause is DuplicateKeyException
    this.addKeyValue("isDuplicateKeyException", isDuplicateKeyException)

    if (isDuplicateKeyException) {
        val duplicateKeyException = (if (ex is DuplicateKeyException) ex else ex.cause) as DuplicateKeyException

        val table =
            duplicateKeyException.message
                ?.substringAfter("INSERT INTO \"")
                ?.substringBefore("\"")
        this.addKeyValue("table", table)

        val constraint =
            duplicateKeyException.cause
                ?.message
                ?.substringAfter("unique constraint \"")
                ?.substringBefore("\"")
        this.addKeyValue("constraint", constraint)
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
    addKeyValue("operation", operationName)
    val start = System.currentTimeMillis()

    try {
        val ret = f(this)
        addKeyValue("success", true)
        setMessage("$operationName successful")
        return ret
    } catch (ex: Exception) {
        addKeyValue("success", false)
        addIsDuplicateKeyException(ex)
        setCause(ex)
        setMessage("$operationName failed")
        throw ex
    } finally {
        val elapsed = System.currentTimeMillis() - start
        addKeyValue("duration_ms", elapsed)
        log()
    }
}
