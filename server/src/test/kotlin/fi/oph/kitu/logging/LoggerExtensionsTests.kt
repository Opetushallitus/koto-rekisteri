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
        assertNotNull(success)
        assertTrue(success)

        val message = event.messages.first { m -> m == "unit-test successful" }
        assertNotNull(message)
        assertEquals("unit-test successful", message)

        val durationMs = event.getValueOrNullByKey<Long>("duration_ms")
        assertNotNull(durationMs)
        assertTrue(durationMs > 0)

        val logsCount = event.logs.size
        assertNotNull(logsCount)
        assertEquals(1, logsCount)
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
        assertNotNull(success)
        assertFalse(success)

        val message = event.messages.first { m -> m == "unit-test failed" }
        assertNotNull(message)
        assertEquals("unit-test failed", message)

        val durationMs = event.getValueOrNullByKey<Long>("duration_ms")
        assertNotNull(durationMs)
        assertTrue(durationMs > 0)

        val logsCount = event.logs.size
        assertNotNull(logsCount)
        assertEquals(1, logsCount)
    }
}
