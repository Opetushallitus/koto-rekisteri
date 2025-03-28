package fi.oph.kitu.logging

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class EventLoggerTests {
    @Test
    fun `addDefaults do add defaults when running the action`() {
        val event = MockEvent()
        val actionToRun = { Thread.sleep(1) }

        // runCatching is eager so it runs the action.
        val logger = EventLogger(runCatching(actionToRun), event)

        // Act
        logger.addDefaults("unit-test")

        val result = logger.getOrThrow()
        assertNotNull(result)

        val success = event.keyValues["success"] as Boolean
        assertNotNull(success, "missing success")
        assertTrue(success, "success should be true")

        val message = event.messages.first { m -> m == "unit-test success" }
        assertNotNull(message)
        assertEquals("unit-test success", message, "expected correct message")

        val logsCount = event.logs.size
        assertNotNull(logsCount, "logs_count should not be null")
        assertEquals(1, logsCount, "logs_count should be 1")
    }

    @Test
    fun `addDefaults do add defaults even if action throws an exception`() {
        class ExceptionThatThrows(
            message: String,
            cause: Throwable? = null,
        ) : Throwable(message, cause)

        val event = MockEvent()

        // Simulates a situation, where we had an action that Returns a failure.
        // For example we had an action that throwed an exception and it was caught and wrapped to Result.failure
        val actionToRun: Result<Unit> =
            runCatching {
                throw ExceptionThatThrows("Expected error")
            }

        val logger = EventLogger(actionToRun, event)

        // Act
        logger.addDefaults("unit-test")

        assertThrows<ExceptionThatThrows> {
            logger.getOrThrow()
        }

        val success = event.keyValues["success"] as Boolean
        assertNotNull(success, "missing success")
        assertFalse(success, "success should be false")

        val message = event.messages.first { m -> m == "unit-test failed" }
        assertNotNull(message)
        assertEquals("unit-test failed", message, "expected correct message")

        val logsCount = event.logs.size
        assertNotNull(logsCount, "logs_count should not be null")
        assertEquals(1, logsCount, "logs_count should be 1")
    }

    @Test
    fun `addDatabaseLogs do database logging when it throws DuplicateKeyException`() {
        val event = MockEvent()
        val actionToRun = {
            throw DuplicateKeyException("INSERT INTO \"my_table\" unique constraint \"my_constraint\"")
        }

        // runCatching is eager so it runs the action.
        val logger = EventLogger(runCatching(actionToRun), event)

        // Act
        logger.addDatabaseLogs()

        assertThrows<DuplicateKeyException> {
            logger.getOrThrow()
        }
    }

    @Test
    fun `withEventAndPerformanceCheck does performance logging when running an action`() {
        val event = MockEvent()
        val logger = MockLogger(event)

        logger
            .atInfo()
            .withEventAndPerformanceCheck {
                // Action runs succesfully
                Thread.sleep(1)
            }.getOrThrow()

        val durationMs = event.keyValues["duration_ms"] as Long
        assertNotNull(durationMs, "duration_ms should not be null")
        assertTrue(durationMs > 0, "duration_ms should be greater than 0")
    }

    @Test
    fun `withEventAndPerformanceCheck does performance logging even if action throws an exception`() {
        class ExceptionThatThrows(
            message: String,
            cause: Throwable? = null,
        ) : Throwable(message, cause)

        val event = MockEvent()
        val logger = MockLogger(event)

        assertThrows<ExceptionThatThrows> {
            logger
                .atInfo()
                .withEventAndPerformanceCheck {
                    Thread.sleep(1)
                    throw ExceptionThatThrows("Expected error")
                }.getOrThrow()
        }

        val durationMs = event.keyValues["duration_ms"] as Long
        assertNotNull(durationMs, "duration_ms should not be null")
        assertTrue(durationMs > 0, "duration_ms should be greater than 0")
    }
}
