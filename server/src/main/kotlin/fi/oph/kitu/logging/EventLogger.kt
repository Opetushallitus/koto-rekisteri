package fi.oph.kitu.logging

import org.slf4j.spi.LoggingEventBuilder

class EventLogger<T>(
    val event: LoggingEventBuilder,
    val action: Result<T>,
) {
    fun withOperation(operationName: String): EventLogger<T> {
        event.add("operationName" to operationName)
        return this
    }

    fun withSuccess(): EventLogger<T> {
        action.onSuccess { event.add("success" to true) }
        action.onFailure { event.add("success" to false) }
        return this
    }

    fun withMessage(operationName: String): EventLogger<T> {
        action.onSuccess { event.setMessage("$operationName success") }
        action.onFailure { event.setMessage("$operationName success") }
        return this
    }

    fun withCause(): EventLogger<T> {
        action.onFailure { ex -> event.setCause(ex) }
        return this
    }

    fun withPerformanceCheck(): EventLogger<T> {
        val start = System.currentTimeMillis()

        action
            .also {
                val elapsed = System.currentTimeMillis() - start
                event.add("duration_ms" to elapsed)
            }

        return this
    }

    fun withDatabaseLogs(): EventLogger<T> {
        action.onFailure { ex -> event.addIsDuplicateKeyException(ex) }
        return this
    }

    fun withLog(): Result<T> = action.also { event.log() }
}

fun <T> LoggingEventBuilder.withEvent(
    operationName: String,
    f: (event: LoggingEventBuilder) -> T,
): T =
    EventLogger(this, runCatching { f(this) })
        .withOperation(operationName)
        .withSuccess()
        .withMessage(operationName)
        .withCause()
        .withPerformanceCheck()
        .withDatabaseLogs()
        .withLog()
        .getOrThrow()
