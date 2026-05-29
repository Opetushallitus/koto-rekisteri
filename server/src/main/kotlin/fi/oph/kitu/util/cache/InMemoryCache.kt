package fi.oph.kitu.util.cache

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

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
                        CacheItem(value, Instant.now().plus(ttl.toJavaDuration()))
                    }
            }?.value
    }

    data class CacheItem<T>(
        val value: T,
        val expiresAt: Instant,
    ) {
        fun isExpired(): Boolean = expiresAt.isBefore(Instant.now())
    }
}
