package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!ci & !e2e")
@ConditionalOnProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"], matchIfMissing = false)
class TehtavapankkiScheduledTasks {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.scheduling.importTehtavapankki.schedule}")
    lateinit var tehtavapankkiImportSchedule: String

    @Bean
    fun dailyImportKotoTehtavapankki(tehtavapankkiService: TehtavapankkiService): Task<Void> =
        Tasks
            .recurring(
                "Koto-import-tehtavapankki",
                Schedules.parseSchedule(tehtavapankkiImportSchedule),
            ).execute { _, _ -> tehtavapankkiService.importTehtavapankki() }
}
