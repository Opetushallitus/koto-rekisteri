package fi.oph.kitu.util

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Skip this property when comparing two objects for equality using equalsIgnoringAnnotated.
 */
@Target(AnnotationTarget.PROPERTY)
@Repeatable
annotation class IgnoreForEquality(
    val context: String,
)

inline fun <reified T : Any> T.getProperties(context: String): List<KProperty1<T, *>> =
    T::class
        .members
        .filterIsInstance<KProperty1<T, *>>()
        .filter { prop -> prop.annotations.none { it is IgnoreForEquality && it.context == context } }

inline fun <reified T : Any> T.equalsIgnoringAnnotated(
    other: T,
    context: String,
): Boolean {
    if (this === other) return true
    val props = getProperties<T>(context)
    return props.all { prop -> propsEqual(prop.get(this), prop.get(other), context) }
}

fun propsEqual(
    a: Any?,
    b: Any?,
    context: String,
): Boolean {
    if (a === b) return true
    if (a == null || b == null) return a == b
    if (a is Collection<*> && b is Collection<*>) {
        return collectionsEqualIgnoringAnnotated(a, b, context)
    }
    return a == b
}

fun collectionsEqualIgnoringAnnotated(
    a: Collection<*>,
    b: Collection<*>,
    context: String,
): Boolean {
    if (a.size != b.size) return false
    val remaining = b.toMutableList()
    return a.all { itemA ->
        if (itemA == null) {
            remaining.remove(null)
        } else {
            val matchIndex =
                remaining.indexOfFirst { itemB ->
                    itemB != null && equalsByProperties(itemA, itemB, context)
                }
            if (matchIndex >= 0) {
                remaining.removeAt(matchIndex)
                true
            } else {
                false
            }
        }
    }
}

fun equalsByProperties(
    a: Any,
    b: Any,
    context: String,
): Boolean {
    if (a === b) return true
    if (a::class != b::class) return false
    val props =
        a::class
            .memberProperties
            .filter { prop -> prop.annotations.none { it is IgnoreForEquality && it.context == context } }
    @Suppress("UNCHECKED_CAST")
    return props.all { prop ->
        val p = prop as KProperty1<Any, *>
        propsEqual(p.get(a), p.get(b), context)
    }
}

inline fun <reified T : Any> T.findDifferentProperties(
    other: T,
    context: String,
): Map<String, Pair<Any?, Any?>> {
    if (this === other) return emptyMap()
    val props = getProperties<T>(context)
    return props
        .flatMap { prop ->
            val thisValue = prop.get(this)
            val thatValue = prop.get(other)
            if (propsEqual(thisValue, thatValue, context)) {
                emptyList()
            } else {
                listOf(prop.name to (thisValue to thatValue))
            }
        }.toMap()
}

fun Map<String, Pair<Any?, Any?>>.ignoreEmptyValues(): Map<String, Pair<Any?, Any?>> =
    filterValues { (thisValue, thatValue) ->
        fun isEmpty(value: Any?): Boolean = value == null || value == "" || (value as? Collection<*>)?.isEmpty() == true
        !isEmpty(thisValue) || !isEmpty(thatValue)
    }
