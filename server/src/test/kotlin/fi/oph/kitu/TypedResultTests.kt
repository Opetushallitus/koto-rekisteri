package fi.oph.kitu

import fi.oph.kitu.TypedResult.Failure
import fi.oph.kitu.TypedResult.Success
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class TypedResultTests {
    class Error

    class ThrowableError : Throwable()

    @Nested
    inner class GetOrNull {
        @Test
        fun `returns the wrapped value on success`() {
            val expected = 42
            val result = Success<_, Error>(expected)

            val actual =
                assertDoesNotThrow {
                    result.getOrNull()
                }

            assertEquals(expected, actual)
        }

        @Test
        fun `returns the null on failure`() {
            val result = Failure<Any, _>(Error())

            val actual =
                assertDoesNotThrow {
                    result.getOrNull()
                }

            assertNull(actual)
        }
    }

    @Nested
    inner class GetOrThrow {
        @Test
        fun `returns the wrapped value on success`() {
            val expected = 42
            val result = Success<_, Error>(expected)

            val actual =
                assertDoesNotThrow {
                    result.getOrThrow()
                }

            assertEquals(expected, actual)
        }

        @Test
        fun `throws the wrapped failure, if the failure is Throwable`() {
            val expected = ThrowableError()
            val result = Failure<Any, _>(expected)

            val actual =
                assertThrows<ThrowableError> {
                    result.getOrThrow()
                }

            assertEquals(expected, actual)
        }

        @Test
        fun `throws IllegalStateException, if failure is not Throwable`() {
            val result = Failure<Any, _>(Error())

            assertThrows<IllegalStateException> {
                result.getOrThrow()
            }
        }
    }

    @Nested
    inner class ErrorOrNull {
        @Test
        fun `returns the wrapped error on failure`() {
            val expected = Error()
            val result = Failure<Any, _>(expected)

            val actual = assertDoesNotThrow { result.errorOrNull() }
            assertEquals(expected, actual)
        }

        @Test
        fun `returns the null on success`() {
            val result = Success<_, Error>(42)

            val actual = assertDoesNotThrow { result.errorOrNull() }
            assertNull(actual)
        }
    }

    @Nested
    inner class Fold {
        @Test
        fun `executes the success callback once, if the result is success`() {
            val expected = 42
            val result = Success<_, Error>(expected)

            var callCount = 0
            result.fold(
                onSuccess = { value: Int ->
                    assertEquals(expected, value, "Success callback invoked with an unexpected value!")
                    callCount++
                },
                onFailure = { /* NOOP */ },
            )

            assertEquals(callCount, 1, "Expected success callback to be called only once!")
        }

        @Test
        fun `does not execute the success callback if the result is failure`() {
            val result = Failure<Any, _>(Error())

            result.fold(
                onSuccess = { value: Any ->
                    fail("Success callback should not have been called!")
                },
                onFailure = { /* NOOP */ },
            )
        }

        @Test
        fun `executes the failure callback once, if the result is failure`() {
            val expected = Error()
            val result = Failure<Any, _>(expected)

            var callCount = 0
            result.fold(
                onSuccess = { /* NOOP */ },
                onFailure = { error: Error ->
                    assertEquals<Error>(expected, error, "Failure callback invoked with an unexpected error!")
                    callCount++
                },
            )

            assertEquals(callCount, 1, "Expected failure callback to be called only once!")
        }

        @Test
        fun `does not execute the failure callback if the result is success`() {
            val result = Success<_, Error>(42)

            result.fold(
                onSuccess = { /* NOOP */ },
                onFailure = { error: Error ->
                    fail("Failure callback should not have been called!")
                },
            )
        }
    }

    @Nested
    inner class OnSuccess {
        @Test
        fun `is executed once if the result is success`() {
            val expected = 42
            val result = Success<_, Error>(expected)

            var callCount = 0
            result.onSuccess { value: Int ->
                assertEquals(expected, value, "Success callback invoked with an unexpected value!")
                callCount++
            }

            assertEquals(callCount, 1, "Expected success callback to be called only once!")
        }

        @Test
        fun `is not executed if the result is failure`() {
            val result = Failure<Any, _>(Error())

            result.onSuccess { value: Any ->
                fail("Success callback should not have been called!")
            }
        }
    }

    @Nested
    inner class OnFailure {
        @Test
        fun `is executed once if the result is failure`() {
            val expected = Error()
            val result = Failure<Any, _>(expected)

            var callCount = 0
            result.onFailure { error: Error ->
                assertEquals<Error>(expected, error, "Failure callback invoked with an unexpected error!")
                callCount++
            }

            assertEquals(callCount, 1, "Expected failure callback to be called only once!")
        }

        @Test
        fun `is not executed if the result is success`() {
            val result = Success<_, Error>(42)

            result.onFailure { error: Error ->
                fail("Failure callback should not have been called!")
            }
        }
    }

    @Nested
    inner class Map {
        @Test
        fun `applies the transformation once if the result is success`() {
            val expectedOriginal = 42
            val expectedFinal = expectedOriginal * 2
            val original = Success<_, Error>(expectedOriginal)

            var callCount = 0
            val transformed =
                original.map { value: Int ->
                    assertEquals(expectedOriginal, value, "Transform callback invoked with an unexpected value!")
                    callCount++

                    value * 2
                }
            assertEquals(callCount, 1, "Expected transform callback to be called only once!")

            val actual = assertDoesNotThrow { transformed.getOrThrow() }
            assertEquals(expectedFinal, actual)
        }

        @Test
        fun `does nothing if the result is failure`() {
            val expected = Error()
            val result = Failure<Any, Error>(expected)

            val transformed =
                result.map {
                    fail("Transform callback should not have been called!")
                }

            val actual = assertDoesNotThrow { transformed.errorOrNull() }
            assertEquals(expected, actual)
        }
    }

    @Nested
    inner class MapFailure {
        @Test
        fun `applies the transformation if the result is failure`() {
            val expectedOriginal = Error()
            val expectedFinal = ThrowableError()
            val original = Failure<Any, Error>(expectedOriginal)

            var callCount = 0
            val transformed =
                original.mapFailure { error: Error ->
                    assertEquals(expectedOriginal, error, "Transform callback invoked with an unexpected value!")
                    callCount++

                    expectedFinal
                }
            assertEquals(callCount, 1, "Expected transform callback to be called only once!")

            val actual = assertDoesNotThrow { transformed.errorOrNull() }
            assertEquals(expectedFinal, actual)
        }

        @Test
        fun `does nothing if the result is success`() {
            val expected = 42
            val result = Success<_, Error>(expected)

            val transformed =
                result.mapFailure {
                    fail("Transform callback should not have been called!")
                }

            val actual = assertDoesNotThrow { transformed.getOrThrow() }
            assertEquals(expected, actual)
        }
    }
}
