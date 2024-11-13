package fi.oph.kitu.logging

import fi.oph.kitu.PeerService
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.ResponseEntity
import java.net.http.HttpResponse

inline fun <reified T> LoggingEventBuilder.addResponse(
    response: HttpResponse<T>,
    peerService: PeerService,
): LoggingEventBuilder =
    this
        .addKeyValue("response.headers", response.headers().toString())
        .addKeyValue("response.body", response.body().toString())
        .addKeyValue("peer.service", peerService.value)

inline fun <reified T> LoggingEventBuilder.addResponse(
    response: ResponseEntity<T>,
    peerService: PeerService,
): LoggingEventBuilder =
    this
        .addKeyValue("response.headers", response.headers)
        .addKeyValue("response.body", response.body)
        .addKeyValue("peer.service", peerService.value)

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
        log()
    }
}
