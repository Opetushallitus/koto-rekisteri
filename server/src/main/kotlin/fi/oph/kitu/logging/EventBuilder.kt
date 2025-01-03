package fi.oph.kitu.logging

import org.slf4j.spi.LoggingEventBuilder

class EventBuilder<T>(
    val action: Result<T>,
    val event: LoggingEventBuilder,
) {
    fun getOrThrow() = action.getOrThrow()

    private fun withOperationName(operationName: String) {
        event.add("operationName" to operationName)
    }

    private fun withSuccessCheck() {
        action.onSuccess { event.add("success" to true) }
        action.onFailure { event.add("success" to false) }
    }

    private fun withMessage(
        onSuccessMessage: String,
        onFailureMessage: String,
    ) {
        action.onSuccess { event.setMessage(onSuccessMessage) }
        action.onFailure { event.setMessage(onFailureMessage) }
    }

    private fun withCause() {
        action.onFailure { ex -> event.setCause(ex) }
    }

    private fun withLog() {
        action.also { event.log() }
    }

    fun withDatabaseLogs() {
        action.onFailure { ex -> event.addIsDuplicateKeyException(ex) }
    }

    fun withDefaultLogging(operationName: String) {
        withOperationName(operationName)
        withSuccessCheck()
        withMessage(
            onSuccessMessage = "$operationName success",
            onFailureMessage = "$operationName failed",
        )
        withCause()
        withLog()
    }
}

fun <T> LoggingEventBuilder.withEventAndPerformanceCheck(action: (LoggingEventBuilder) -> T): EventBuilder<T> {
    val start = System.currentTimeMillis()
    val result =
        runCatching { action(this) }
            .also {
                val elapsed = System.currentTimeMillis() - start
                add("duration_ms" to elapsed)
            }

    return EventBuilder(result, this)
}

// Wrapper for old calls
fun <T> LoggingEventBuilder.withEvent(
    operationName: String,
    action: (LoggingEventBuilder) -> T,
) = withEventAndPerformanceCheck(action)
    .apply {
        withDefaultLogging(operationName)
        withDatabaseLogs()
    }.getOrThrow()
