package fi.oph.kitu

import arrow.core.Either
import kotlin.test.assertEquals
import kotlin.test.assertTrue

inline fun <reified ExpectedError : Throwable> assertLeftIsThrowable(
    either: Either<*, *>,
    errorMessage: String?,
) {
    assertTrue(either.isLeft())
    assertTrue(either is Either.Left)
    assertTrue(either.value is ExpectedError)
    assertEquals((either.value as ExpectedError).message, errorMessage)
}
