package fi.oph.kitu.kotoutumiskoulutus

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
@ConditionalOnProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"], matchIfMissing = false)
class KoealustaScheduledTasks {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.scheduling.import.schedule}")
    lateinit var koealustaImportSchedule: String

    @Bean
    fun dailyImportKotoSuoritukset(koealustaService: KoealustaService): Task<Instant> =
        Tasks
            .recurring("Koto-import", Schedules.parseSchedule(koealustaImportSchedule), Instant::class.java)
            .initialData(Instant.EPOCH)
            .executeStateful { taskInstance, _ ->
                koealustaService.importSuoritukset(taskInstance.data)
            }
}
