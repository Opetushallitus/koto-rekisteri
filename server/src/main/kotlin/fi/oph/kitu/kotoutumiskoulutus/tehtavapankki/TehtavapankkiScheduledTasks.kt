package fi.oph.kitu.kotoutumiskoulutus.tehtavapankki

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ExtendedSchedules
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!ci & !e2e & !test")
@ConditionalOnBooleanProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"])
class TehtavapankkiScheduledTasks(
    private val tracer: Tracer,
) {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.scheduling.importTehtavapankki.schedule}")
    var tehtavapankkiImportSchedule: String? = null

    @Bean
    fun dailyImportKotoTehtavapankki(tehtavapankkiService: TehtavapankkiService): Task<Void> =
        Tasks
            .recurring(
                "Koto-import-tehtavapankki",
                ExtendedSchedules.parse(tehtavapankkiImportSchedule),
            ).execute { _, _ ->
                tracer
                    .spanBuilder("TehtavapankkiScheduledTasks.dailyImportKotoTehtavapankki.tasks.execute")
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "Koto-import-tehtavapankki")
                        tehtavapankkiService.importTehtavapankki()
                    }
            }
}
