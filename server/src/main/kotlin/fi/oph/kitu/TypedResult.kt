package fi.oph.kitu

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import kotlinx.html.B

sealed class TypedResult<Value, Error> {
    data class Success<Value, Error>(
        val value: Value,
    ) : TypedResult<Value, Error>()

    data class Failure<Value, Error>(
        val error: Error,
    ) : TypedResult<Value, Error>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

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

    fun errorOrNull(): Error? =
        when (this) {
            is Success -> null
            is Failure -> this.error
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

    fun <NewValue> flatMap(transform: (Value) -> TypedResult<NewValue, Error>): TypedResult<NewValue, Error> =
        when (this) {
            is Success<Value, Error> -> transform(this.value)
            is Failure<Value, Error> -> Failure(this.error)
        }

    companion object {
        inline fun <Value> runCatching(block: () -> Value): TypedResult<Value, Throwable> =
            try {
                Success(block())
            } catch (e: Throwable) {
                Failure(e)
            }
    }
}

fun <T> Span.setAttributesForTypedResult(result: T) {
    this.setAttribute("isTypedResult", result is TypedResult<*, *>)
    if (result is TypedResult<*, *>) {
        this.setAttribute("TypedResult.isSuccess", result.isSuccess)
        if (result.isFailure) {
            this.setStatus(StatusCode.ERROR)
            val error = (result as TypedResult.Failure<*, *>).error
            if (error is Throwable) {
                this.recordException(error)
            }
        }
    }
}

fun <Value, Error> Iterable<TypedResult<Value, out Error>>.splitIntoValuesAndErrors(): Pair<List<Value>, List<Error>> {
    val values =
        this
            .filterIsInstance<TypedResult.Success<Value, Error>>()
            .map { it.value }

    val errors =
        this
            .filterIsInstance<TypedResult.Failure<Value, Error>>()
            .map { it.error }

    return values to errors
}

fun <T> Result<T>.toTypedResult(): TypedResult<T, Throwable> =
    this.fold({
        TypedResult.Success(it)
    }, { TypedResult.Failure(it) })

fun <T, E> Result<T>.toTypedResult(mapFailure: (Throwable) -> E): TypedResult<T, E> =
    this.fold({
        TypedResult.Success(it)
    }, { TypedResult.Failure(mapFailure(it)) })

inline fun <reified A, B> List<TypedResult<A, B>>.partitionBySuccess(): Pair<List<A>, List<B>> {
    val (success, failed) = this.partition { it.isSuccess }
    return success.mapNotNull { it.getOrNull() } to failed.mapNotNull { it.errorOrNull() }
}

inline fun <reified A, B, E> List<TypedResult<A, E>>.mapValues(crossinline f: (A) -> B): List<TypedResult<B, E>> =
    map {
        it.fold(
            { s -> TypedResult.Success(f(s)) },
            { e -> TypedResult.Failure(e) },
        )
    }
