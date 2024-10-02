package fi.oph.kitu.yki

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalTime

@Configuration
@ConditionalOnProperty(name = ["kitu.yki.scheduling.enabled"], matchIfMissing = false)
class YkiScheduledTasks {
    @Bean
    fun dailyImport(ykiService: YkiService): Task<Void> =
        Tasks
            .recurring("YKI-import-daily", Schedules.daily(LocalTime.of(3, 0)))
            .execute { _, _ -> ykiService.importYkiSuoritukset() }
}
