package fi.oph.kitu

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(
        onSuccess = { value -> transform(value) },
        onFailure = { error -> Result.failure<R>(error) },
    )

inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { value -> Result.success(value) },
        onFailure = { throwable -> Result.failure(transform(throwable)) },
    )
