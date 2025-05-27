package fi.oph.kitu.kotoutumiskoulutus

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ExtendedSchedules
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"])
class KoealustaScheduledTasks {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.scheduling.import.schedule}")
    lateinit var koealustaImportSchedule: String

    @Bean
    fun dailyImportKotoSuoritukset(
        koealustaService: KoealustaService,
        tracer: Tracer,
    ): Task<Instant> =
        Tasks
            .recurring(
                "Koto-import",
                ExtendedSchedules.parse(koealustaImportSchedule),
                Instant::class.java,
            ).initialData(Instant.EPOCH)
            .executeStateful { taskInstance, _ ->
                tracer
                    .spanBuilder("koealusta.scheduler.task.import.suoritukset")
                    .setAttribute("taskInstanceId", taskInstance.id)
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "Koto-import-suoritukset")
                        koealustaService.importSuoritukset(taskInstance.data)
                    }
            }
}
