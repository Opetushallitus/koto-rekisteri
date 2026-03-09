package fi.oph.kitu

import kotlin.reflect.KProperty1

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
    return props.all { prop -> prop.get(this) == prop.get(other) }
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
