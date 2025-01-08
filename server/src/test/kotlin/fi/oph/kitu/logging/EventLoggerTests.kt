package fi.oph.kitu.logging

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull

class EventLoggerTests {
    @Test
    fun `withEventAndPerformanceCheck does performance logging when running an action`() {
        val event = MockEvent()
        val logger = MockLogger(event)

        logger.atInfo().withEventAndPerformanceCheck {
            // Action runs succesfully
            Thread.sleep(1)
        }

        val durationMs = event.getValueOrNullByKey<Long>("duration_ms")
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
            logger.atInfo().withEvent("unit-test") {
                throw ExceptionThatThrows("Expected error")
            }
        }

        val durationMs = event.getValueOrNullByKey<Long>("duration_ms")
        assertNotNull(durationMs, "duration_ms should not be null")
        assertTrue(durationMs > 0, "duration_ms should be greater than 0")
    }
}
