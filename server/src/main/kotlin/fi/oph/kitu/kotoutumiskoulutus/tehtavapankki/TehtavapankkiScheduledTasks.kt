package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ExtendedSchedules
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!ci & !e2e & !test")
@ConditionalOnProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"], matchIfMissing = false)
class TehtavapankkiScheduledTasks {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.scheduling.importTehtavapankki.schedule}")
    var tehtavapankkiImportSchedule: String? = null

    @Bean
    fun dailyImportKotoTehtavapankki(tehtavapankkiService: TehtavapankkiService): Task<Void> =
        Tasks
            .recurring(
                "Koto-import-tehtavapankki",
                ExtendedSchedules.parse(tehtavapankkiImportSchedule),
            ).execute { _, _ -> tehtavapankkiService.importTehtavapankki() }
}
