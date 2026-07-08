package fi.oph.kitu.i18n

import java.util.concurrent.ConcurrentHashMap

object UiTextRegistry {
    private val registry = ConcurrentHashMap<String, String>()

    fun record(
        key: String,
        fi: String,
    ) {
        registry[key] = fi
    }

    fun all(): Map<String, String> = registry.toMap()
}
