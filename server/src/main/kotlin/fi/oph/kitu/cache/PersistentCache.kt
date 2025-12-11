package fi.oph.kitu.cache

import fi.oph.kitu.defaultObjectMapper
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PersistentCache(
    val jdbcTemplate: JdbcTemplate,
) {
    val defaultTtlMinutes: Long = 60 * 12

    @WithSpan
    fun <T> getAsap(
        valueType: Class<T>,
        key: String? = null,
        ttl: Long = defaultTtlMinutes,
        fetchValue: () -> T?,
    ): T? {
        val storeKey = key ?: valueType.name
        val item = findById(storeKey)
        if (item == null) {
            return save(storeKey, ttl, fetchValue)?.getValue(valueType)
        }
        if (item.expiresAt < LocalDateTime.now()) {
            saveAtBackground(storeKey, fetchValue, ttl)
        }
        return item.getValue(valueType)
    }

    private fun <T> save(
        key: String,
        ttl: Long,
        fetchValue: () -> T?,
    ): CacheItem? =
        fetchValue()?.let { value ->
            upsert(
                CacheItem(
                    key,
                    defaultObjectMapper.writeValueAsString(value),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusMinutes(ttl),
                ),
            )
        }

    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> saveAtBackground(
        key: String,
        fetchValue: () -> T?,
        ttl: Long,
    ) {
        GlobalScope.launch { save(key, ttl, fetchValue) }
    }

    private fun findById(key: String): CacheItem? =
        jdbcTemplate
            .query(
                "SELECT * FROM cache WHERE key = ?",
                CacheItem.fromRow,
                key,
            ).firstOrNull()

    private fun upsert(item: CacheItem): CacheItem =
        jdbcTemplate
            .query(
                """
                INSERT INTO cache (key, value, updated_at, expires_at) 
                VALUES (?, ?, ?, ?) 
                ON CONFLICT (key) DO UPDATE SET 
                    value = EXCLUDED.value, 
                    updated_at = EXCLUDED.updated_at, 
                    expires_at = EXCLUDED.expires_at
                RETURNING *;
                """.trimIndent(),
                CacheItem.fromRow,
                item.key,
                item.value,
                item.updatedAt,
                item.expiresAt,
            ).first()
}

data class CacheItem(
    val key: String,
    val value: String,
    val updatedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
) {
    fun <T> getValue(valueType: Class<T>): T = defaultObjectMapper.readValue(value, valueType)

    companion object {
        val fromRow: RowMapper<CacheItem> =
            RowMapper { rs, _ ->
                CacheItem(
                    key = rs.getString("key"),
                    value = rs.getString("value"),
                    updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
                    expiresAt = rs.getTimestamp("expires_at").toLocalDateTime(),
                )
            }
    }
}
