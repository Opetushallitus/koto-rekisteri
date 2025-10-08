package fi.oph.kitu

inline fun <reified T> List<T>.growToSize(
    expectedSize: Int,
    value: T,
): List<T> =
    if (expectedSize > size) {
        this + List(expectedSize - size) { value }
    } else {
        this
    }

inline fun <reified T> List<T>.intersects(other: List<T>): Boolean = intersect(other).isNotEmpty()
