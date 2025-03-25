package fi.oph.kitu

sealed class TypedResult<Value, Error> {
    data class Success<Value, Error>(
        val value: Value,
    ) : TypedResult<Value, Error>()

    data class Failure<Value, Error>(
        val error: Error,
    ) : TypedResult<Value, Error>()

    fun getOrNull(): Value? =
        when (this) {
            is Success -> this.value
            is Failure -> null
        }

    fun getOrThrow(): Value =
        when (this) {
            is Success -> this.value
            is Failure ->
                when (this.error) {
                    is Throwable -> throw this.error
                    else -> throw IllegalStateException("Tried to get value of a failed result: ${this.error}")
                }
        }

    fun <NewValue> fold(
        onSuccess: (Value) -> NewValue,
        onFailure: (Error) -> NewValue,
    ): NewValue =
        when (this) {
            is Success<Value, *> -> onSuccess(this.value)
            is Failure<*, Error> -> onFailure(this.error)
        }

    fun onSuccess(onSuccess: (Value) -> Unit): TypedResult<Value, Error> {
        when (this) {
            is Success<Value, *> -> onSuccess(this.value)
            is Failure<*, *> -> {}
        }

        return this
    }

    fun onFailure(onFailure: (Error) -> Unit): TypedResult<Value, Error> {
        when (this) {
            is Success<*, *> -> {}
            is Failure<*, Error> -> onFailure(this.error)
        }

        return this
    }

    fun <NewValue> map(transform: (Value) -> NewValue): TypedResult<NewValue, Error> =
        fold(
            onSuccess = { Success(transform(it)) },
            onFailure = { Failure(it) },
        )

    fun <NewError> mapFailure(transform: (Error) -> NewError): TypedResult<Value, NewError> =
        fold(
            onSuccess = { Success(it) },
            onFailure = { Failure(transform(it)) },
        )
}
