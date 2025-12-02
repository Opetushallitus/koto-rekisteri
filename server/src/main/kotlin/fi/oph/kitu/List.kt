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

fun <T> List<T>.removeAtIndex(index: Int): List<T> = this.filterIndexed { i, _ -> i != index }

fun <T> List<T>.splitAt(index: Int): Pair<List<T>, List<T>> = this.subList(0, index) to this.subList(index, this.size)
