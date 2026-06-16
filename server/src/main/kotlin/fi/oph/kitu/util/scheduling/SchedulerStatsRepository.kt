package fi.oph.kitu.util.scheduling

import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class SchedulerStatsRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    @WithSpan
    fun countCurrentlyRunning(): Long =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scheduled_tasks WHERE picked = true",
            Long::class.java,
        ) ?: 0

    @WithSpan
    fun countCurrentlyFailing(): Long =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scheduled_tasks WHERE consecutive_failures > 0",
            Long::class.java,
        ) ?: 0
}
