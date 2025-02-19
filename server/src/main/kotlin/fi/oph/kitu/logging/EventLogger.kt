package fi.oph.kitu.logging

import org.slf4j.spi.LoggingEventBuilder
import org.springframework.dao.DuplicateKeyException

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

    /** Adds the "operationName" key with given value. */
    private fun addKeyOperationName(operationName: String) {
        event.add("operationName" to operationName)
    }

    /** Adds key "success" with boolean whether the action succeed or not. */
    private fun checkSuccess() {
        result.onSuccess { event.add("success" to true) }
        result.onFailure { event.add("success" to false) }
    }

    /**
     * Sets success message when the action is success.
     * Sets error message when the action fails.
     * */
    private fun setMessage(
        onSuccessMessage: String,
        onFailureMessage: String,
    ) {
        result.onSuccess { event.setMessage(onSuccessMessage) }
        result.onFailure { event.setMessage(onFailureMessage) }
    }

    /** When action fails, a cause will be logged. */
    private fun setCause() {
        result.onFailure { ex -> event.setCause(ex) }
    }

    /** Performs the actual logging. */
    private fun performLog() {
        result.also { event.log() }
    }

    /** Adds database related logging, such as checking if the error caused by [org.springframework.dao.DuplicateKeyException] */
    fun addDatabaseLogs() {
        result.onFailure { ex -> checkDuplicateKeyException(ex) }
    }

    /** adds key/value that indicates whether the error was caused by [org.springframework.dao.DuplicateKeyException]. */
    private fun checkDuplicateKeyException(ex: Throwable) {
        val isDuplicateKeyException = ex is DuplicateKeyException || ex.cause is DuplicateKeyException

        if (!event.addCondition("isDuplicateKeyException", isDuplicateKeyException)) {
            return
        }

        val duplicateKeyException = (if (ex is DuplicateKeyException) ex else ex.cause) as DuplicateKeyException

        val table =
            duplicateKeyException.cause
                ?.message
                ?.substringAfter("INSERT INTO \"")
                ?.substringBefore("\"")

        val constraint =
            duplicateKeyException.cause
                ?.message
                ?.substringAfter("unique constraint \"")
                ?.substringBefore("\"")

        event.add(
            "table" to table,
            "constraint" to constraint?.trim(),
        )
    }

    /**
     * Adds defaults to logging which are:
     *  - [EventLogger.addKeyOperationName]
     *  - [EventLogger.checkSuccess]
     *  - [EventLogger.setMessage]
     *  - [EventLogger.setCause]
     *  - [EventLogger.performLog]
     */
    fun addDefaults(operationName: String) {
        addKeyOperationName(operationName)
        checkSuccess()
        setMessage(
            onSuccessMessage = "$operationName success",
            onFailureMessage = "$operationName failed",
        )
        setCause()
        performLog()
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
