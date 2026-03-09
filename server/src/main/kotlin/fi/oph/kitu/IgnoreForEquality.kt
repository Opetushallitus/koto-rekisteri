package fi.oph.kitu

import kotlin.reflect.KProperty1

/**
 * Skip this property when comparing two objects for equality using equalsIgnoringAnnotated.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class IgnoreForEquality

inline fun <reified T : Any> T.getProperties(): List<KProperty1<T, *>> =
    T::class
        .members
        .filterIsInstance<kotlin.reflect.KProperty1<T, *>>()
        .filter { prop -> prop.annotations.none { it is IgnoreForEquality } }

inline fun <reified T : Any> T.equalsIgnoringAnnotated(other: T): Boolean {
    if (this === other) return true
    val props = getProperties<T>()
    return props.all { prop -> prop.get(this) == prop.get(other) }
}

inline fun <reified T : Any> T.findDifferentProperties(other: T): Map<String, Pair<Any?, Any?>> {
    if (this === other) return emptyMap()
    val props = getProperties<T>()
    return props
        .flatMap { prop ->
            val thisValue = prop.get(this)
            val thatValue = prop.get(other)
            if (thisValue == thatValue) {
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
