package fi.oph.kitu.logging

import org.slf4j.spi.LoggingEventBuilder

/**
 * A class to specify what to log to an event before, during or after, the specified lambda call.
 */
class EventLogger<T>(
    /** Result of an action that will be wrapped with event logs. */
    val result: Result<T>,
    /** Event builder, that logs, sets message, adds key-value, etc, around the action. */
    val event: LoggingEventBuilder,
) {
    /** Gets the value from the underlying action */
    fun getOrThrow() = result.getOrThrow()

    /** Sets the "operationName" key with given value. */
    private fun withOperationName(operationName: String) {
        event.add("operationName" to operationName)
    }

    /** Adds key "success" with boolean whether the action succeed or not. */
    private fun withSuccessCheck() {
        result.onSuccess { event.add("success" to true) }
        result.onFailure { event.add("success" to false) }
    }

    /**
     * Sets success message when the action is success.
     * Sets error message when the action fails.
     * */
    private fun withMessage(
        onSuccessMessage: String,
        onFailureMessage: String,
    ) {
        result.onSuccess { event.setMessage(onSuccessMessage) }
        result.onFailure { event.setMessage(onFailureMessage) }
    }

    /** When action fails, a cause will be logged. */
    private fun withCause() {
        result.onFailure { ex -> event.setCause(ex) }
    }

    /** Performs the actual logging. */
    private fun withLog() {
        result.also { event.log() }
    }

    /** Adds database related logging, such as checking if the error caused by a [org.springframework.dao.DuplicateKeyException] */
    fun withDatabaseLogs() {
        result.onFailure { ex -> event.addIsDuplicateKeyException(ex) }
    }

    /**
     * Adds defaults to logging which are:
     *  - [EventLogger.withOperationName]
     *  - [EventLogger.withSuccessCheck]
     *  - [EventLogger.withMessage]
     *  - [EventLogger.withCause]
     *  - [EventLogger.withLog]
     */
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

/** Runs the given lambda action and measure it's performance */
fun <T> LoggingEventBuilder.withEventAndPerformanceCheck(action: (LoggingEventBuilder) -> T): EventLogger<T> {
    val start = System.currentTimeMillis()
    val result =
        runCatching { action(this) }
            .also {
                val elapsed = System.currentTimeMillis() - start
                add("duration_ms" to elapsed)
            }

    return EventLogger(result, this)
}

fun <T> LoggingEventBuilder.withEvent(
    operationName: String,
    action: (LoggingEventBuilder) -> T,
) = withEventAndPerformanceCheck(action)
    .apply {
        withDefaultLogging(operationName)
        withDatabaseLogs()
    }.getOrThrow()
