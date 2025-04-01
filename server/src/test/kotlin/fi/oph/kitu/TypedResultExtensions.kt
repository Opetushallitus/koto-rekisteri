package fi.oph.kitu

/**
 * Specify that the result is expected to be a success. If it is a failure, the error will be thrown.
 */
fun <Value, Error> TypedResult<Value, *>.mustBeSuccess(message: String = "must be success"): TypedResult<Value, Error> =
    TypedResult.Success(getOrThrow())
