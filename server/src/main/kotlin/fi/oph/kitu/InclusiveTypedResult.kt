package fi.oph.kitu

sealed class InclusiveTypedResult<Value, Error> {
    data class Success<Value, Error>(
        val value: Value,
    ) : InclusiveTypedResult<Value, Error>()

    data class Failure<Value, Error>(
        val error: Error,
    ) : InclusiveTypedResult<Value, Error>()

    data class Both<Value, Error>(
        val value: Value,
        val error: Error,
    ) : InclusiveTypedResult<Value, Error>()

    fun <NewValue> mapSuccess(transform: (Value) -> NewValue): InclusiveTypedResult<NewValue, Error> =
        when (this) {
            is Success -> Success(transform(value))
            is Failure -> Failure(error)
            is Both -> Both(transform(value), error)
        }

    fun <NewError> mapFailure(transform: (Error) -> NewError): InclusiveTypedResult<Value, NewError> =
        when (this) {
            is Success -> Success(value)
            is Failure -> Failure(transform(error))
            is Both -> Both(value, transform(error))
        }

    companion object {
        inline fun <Value> runCatching(block: () -> Value): InclusiveTypedResult<Value, Throwable> =
            try {
                Success(block())
            } catch (e: Throwable) {
                Failure(e)
            }
    }
}

fun <Value, Error> List<InclusiveTypedResult<Value, Error>>.partitionInclusiveTypedResult():
    Pair<List<Value>, List<Error>> =
    fold(emptyList<Value>() to emptyList<Error>()) { (values, rights), ior ->
        when (ior) {
            is InclusiveTypedResult.Success -> (values + ior.value) to rights
            is InclusiveTypedResult.Failure -> values to (rights + ior.error)
            is InclusiveTypedResult.Both -> (values + ior.value) to (rights + ior.error)
        }
    }
