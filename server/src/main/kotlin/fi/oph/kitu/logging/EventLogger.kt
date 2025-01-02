package fi.oph.kitu.logging

import org.slf4j.spi.LoggingEventBuilder

class EventLogger<T>(
    val event: LoggingEventBuilder,
    val action: Result<T>,
) {
    fun logOperation(operationName: String): EventLogger<T> {
        event.add("operation" to operationName)
        action
            .onSuccess {
                event.add("success" to true)
                event.setMessage("$operationName success")
            }.onFailure { ex ->
                event.add("success" to false)
                event.setCause(ex)
                event.setMessage("$operationName failed")
            }

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

    fun runAndLog(): Result<T> = action.also { event.log() }

    fun <T> LoggingEventBuilder.withEventResult(
        operationName: String,
        f: (event: LoggingEventBuilder) -> T,
    ): T =
        EventLogger(this, runCatching { f(this) })
            .logOperation(operationName)
            .withPerformanceCheck()
            .withDatabaseLogs()
            .runAndLog()
            .getOrThrow()
}
