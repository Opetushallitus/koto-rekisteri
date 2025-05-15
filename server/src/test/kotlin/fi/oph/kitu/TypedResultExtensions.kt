package fi.oph.kitu

import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Specify that the result is expected to be a success. If it is a failure, the error will be thrown.
 */
fun <Value, Error> TypedResult<Value, *>.mustBeSuccess(message: String = "must be success"): TypedResult<Value, Error> =
    TypedResult.Success(getOrThrow())

inline fun <reified ExpectedError : Throwable> assertFailureIsThrowable(
    typedResult: TypedResult<*, *>,
    errorMessage: String?,
) {
    assertTrue(typedResult.isFailure)
    assertTrue(typedResult is TypedResult.Failure)
    assertTrue(typedResult.error is ExpectedError)
    assertEquals(typedResult.error.message, errorMessage)
}
