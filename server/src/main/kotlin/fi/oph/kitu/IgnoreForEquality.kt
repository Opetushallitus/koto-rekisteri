package fi.oph.kitu

/**
 * Skip this property when comparing two objects for equality using equalsIgnoringAnnotated.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class IgnoreForEquality

inline fun <reified T : Any> T.equalsIgnoringAnnotated(other: T): Boolean {
    if (this === other) return true

    val props =
        T::class
            .members
            .filterIsInstance<kotlin.reflect.KProperty1<T, *>>()
            .filter { prop -> prop.annotations.none { it is IgnoreForEquality } }

    return props.all { prop -> prop.get(this) == prop.get(other) }
}
