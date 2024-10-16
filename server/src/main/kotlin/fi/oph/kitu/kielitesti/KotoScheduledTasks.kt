package fi.oph.kitu.kielitesti

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
@ConditionalOnProperty(name = ["kitu.kielitesti.scheduling.enabled"], matchIfMissing = false)
class KotoScheduledTasks {
    @Value("\${kitu.kielitesti.scheduling.import.schedule}")
    lateinit var kielitestiImportSchedule: String

    @Bean
    fun dailyImportKotoSuoritukset(moodleService: MoodleService): Task<Instant> =
        Tasks
            .recurring("Kielitesti-import", Schedules.parseSchedule(kielitestiImportSchedule), Instant::class.java)
            .initialData(Instant.EPOCH)
            .executeStateful { taskInstance, _ -> moodleService.importSuoritukset(taskInstance.data) }
}
