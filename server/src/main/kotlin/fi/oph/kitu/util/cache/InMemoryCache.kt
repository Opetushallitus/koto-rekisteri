package fi.oph.kitu.util.cache

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class InMemoryCache<I, O>(
    val ttl: Duration,
    val fn: (I) -> O?,
) {
    private val items = ConcurrentHashMap<I, CacheItem<O>>()

    fun get(key: I): O? {
        items[key]?.takeIf { !it.isExpired() }?.let { return it.value }
        return items
            .compute(key) { _, current ->
                current?.takeIf { !it.isExpired() }
                    ?: fn(key)?.let { value ->
                        CacheItem(value, LocalDateTime.now().plusSeconds(ttl.inWholeSeconds))
                    }
            }?.value
    }

    data class CacheItem<T>(
        val value: T,
        val expiresAt: LocalDateTime,
    ) {
        fun isExpired(): Boolean = expiresAt.isBefore(LocalDateTime.now())
    }
}
