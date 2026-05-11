package fi.oph.kitu.util.result

import arrow.core.Either

/**
 * Palauttaa Right-arvon, tai heittää Left-arvon (jos se on Throwable),
 * tai käärii sen IllegalStateExceptioniin.
 */
fun <T> Either<*, T>.getOrThrow(): T =
    when (this) {
        is Either.Right -> {
            value
        }

        is Either.Left -> {
            when (val left = value) {
                is Throwable -> throw left
                else -> throw IllegalStateException("Tried to get value of a Left: $left")
            }
        }
    }

fun <E, V> Iterable<Either<E, V>>.splitIntoValuesAndErrors(): Pair<List<V>, List<E>> {
    val values = mutableListOf<V>()
    val errors = mutableListOf<E>()
    for (e in this) {
        when (e) {
            is Either.Right -> values += e.value
            is Either.Left -> errors += e.value
        }
    }
    return values to errors
}
