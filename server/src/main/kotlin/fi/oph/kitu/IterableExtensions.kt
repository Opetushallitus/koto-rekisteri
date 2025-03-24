package fi.oph.kitu

fun <T> Iterable<T>.only(predicate: (T) -> Boolean): T {
    val found = this.filter(predicate)
    if (found.size != 1) {
        throw IllegalStateException("List must have only 1 element, but it had ${found.size} element(s).")
    }

    return found.first()
}

/**
 * Gets the value, pair.second by it's key, pair.first from list of pairs.
 *
 * Returns null if the value is not found.
 */
fun <TKey, TValue> Iterable<Pair<TKey, TValue>>.getValueOrNull(key: TKey): TValue? {
    val pairs = this.filter { pair -> pair.first == key }

    if (pairs.size > 1) {
        throw java.lang.IllegalStateException(
            "Can't decide which value to use, because there was more than one value with the same key '$key'.",
        )
    }

    return pairs.firstOrNull { it.first == key }?.second
}

/**
 * Gets the value, pair.second by it's key, pair.first from list of pairs.
 *
 * Returns an empty string value if the value is not found.
 */
fun <TKey> Iterable<Pair<TKey, String?>>.getValueOrEmpty(key: TKey): String = this.getValueOrNull(key) ?: ""
