package fi.oph.kitu

import java.time.LocalDateTime
import kotlin.time.Duration

class Cache<I, O>(
    val ttl: Duration,
    val fn: (I) -> O?,
) {
    private val items = mutableMapOf<I, CacheItem<O>>()

    fun get(key: I): O? {
        val now = LocalDateTime.now()

        val cachedItem = items[key]
        if (cachedItem != null) {
            if (cachedItem.expiresAt.isBefore(now)) {
                items.remove(key)
            } else {
                return cachedItem.value
            }
        }

        return fn(key)?.also {
            items[key] = CacheItem(it, now.plusSeconds(ttl.inWholeSeconds))
        }
    }

    data class CacheItem<T>(
        val value: T,
        val expiresAt: LocalDateTime,
    )
}
