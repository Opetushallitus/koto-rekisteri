package fi.oph.kitu.logging

import fi.oph.kitu.PeerService
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.ResponseEntity
import java.net.http.HttpResponse

inline fun <reified T> LoggingEventBuilder.addResponse(
    peerService: PeerService,
    endpoint: String,
    response: HttpResponse<T>,
): LoggingEventBuilder =
    addResponse<T>(peerService, endpoint, response.statusCode(), response.headers(), response.body())

inline fun <reified T> LoggingEventBuilder.addResponse(
    peerService: PeerService,
    endpoint: String,
    response: ResponseEntity<T>,
): LoggingEventBuilder = addResponse<T>(peerService, endpoint, response.statusCode, response.headers, response.body)

inline fun <reified T> LoggingEventBuilder.addResponse(
    peerService: PeerService,
    endpoint: String,
    responseStatus: Any,
    responseHeaders: Any,
    responseBody: Any?,
): LoggingEventBuilder =
    this
        .addKeyValue("$peerService.$endpoint.response.status", responseStatus)
        .addKeyValue("$peerService.$endpoint.response.headers", responseHeaders)
        .addKeyValue("$peerService.$endpoint.response.body", responseBody)
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
