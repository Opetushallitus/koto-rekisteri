package fi.oph.kitu.util.scheduling

import fi.oph.kitu.DBContainerConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertTrue

@SpringBootTest
@Import(DBContainerConfiguration::class)
class SchedulerStatsRepositoryTest(
    @param:Autowired private val repository: SchedulerStatsRepository,
    @param:Autowired private val jdbc: JdbcTemplate,
) {
    @Test
    fun `laskee kaynnissa olevat ja virhetilassa olevat erajaot`() {
        val runningBefore = repository.countCurrentlyRunning()
        val failingBefore = repository.countCurrentlyFailing()

        jdbc.update(
            "INSERT INTO scheduled_tasks (task_name, task_instance, execution_time, picked, version) " +
                "VALUES (?, ?, now(), true, 1)",
            "test-running-task",
            "test-running-instance",
        )
        jdbc.update(
            "INSERT INTO scheduled_tasks " +
                "(task_name, task_instance, execution_time, picked, consecutive_failures, version) " +
                "VALUES (?, ?, now(), false, 3, 1)",
            "test-failing-task",
            "test-failing-instance",
        )

        try {
            assertTrue(repository.countCurrentlyRunning() >= runningBefore + 1)
            assertTrue(repository.countCurrentlyFailing() >= failingBefore + 1)
        } finally {
            jdbc.update("DELETE FROM scheduled_tasks WHERE task_name IN ('test-running-task', 'test-failing-task')")
        }
    }
}
