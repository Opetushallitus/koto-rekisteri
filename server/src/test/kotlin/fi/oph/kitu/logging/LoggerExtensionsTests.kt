package fi.oph.kitu.logging

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LoggerExtensionsTests {
    @Test
    fun `withEvent logs success 'true' when action does not throw exception`() {
        val event = MockEvent()
        val logger = MockLogger(event)

        logger.atInfo().withEvent("unit-test") {
            // Action runs succesfully
            Thread.sleep(1)
        }

        val success = event.getValueOrNullByKey<Boolean>("success")
        assertNotNull(success, "missing success")
        assertTrue(success, "success should be true")

        val message = event.messages.first { m -> m == "unit-test success" }
        assertNotNull(message)
        assertEquals("unit-test success", message, "expected correct message")

        val durationMs = event.getValueOrNullByKey<Long>("duration_ms")
        assertNotNull(durationMs, "duration_ms should not be null")
        assertTrue(durationMs > 0, "duration_ms should be greater than 0")

        val logsCount = event.logs.size
        assertNotNull(logsCount, "logs_count should not be null")
        assertEquals(1, logsCount, "logs_count should be 1")
    }

    @Test
    fun `withEvent logs success 'false' when action throws exception`() {
        class ExceptionThatThrows(
            message: String,
            cause: Throwable? = null,
        ) : Throwable(message, cause)

        val event = MockEvent()
        val logger = MockLogger(event)

        assertThrows<ExceptionThatThrows> {
            logger.atInfo().withEvent("unit-test") {
                Thread.sleep(1) // sleep 1ms in order to have key 'duration_ms'  larger than 1.
                throw ExceptionThatThrows("Expected error")
            }
        }

        val success = event.getValueOrNullByKey<Boolean>("success")
        assertNotNull(success, "success should not be null")
        assertFalse(success, "success should be false")

        val message = event.messages.first { m -> m == "unit-test failed" }
        assertNotNull(message)
        assertEquals("unit-test failed", message, "expected correct message")

        val durationMs = event.getValueOrNullByKey<Long>("duration_ms")
        assertNotNull(durationMs, "duration_ms should not be null")
        assertTrue(durationMs > 0, "duration_ms should be greater than 0")

        val logsCount = event.logs.size
        assertNotNull(logsCount, "logs_count should not be null")
        assertEquals(1, logsCount, "logs_count should be 1")
    }
}
