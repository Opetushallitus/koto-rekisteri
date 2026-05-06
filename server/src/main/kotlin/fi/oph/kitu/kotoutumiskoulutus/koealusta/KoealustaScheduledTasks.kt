package fi.oph.kitu.kotoutumiskoulutus.koealusta

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.observability.recurringStatefulTask
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"])
class KoealustaScheduledTasks(
    private val tracer: Tracer,
) {
    @Value("\${kitu.kotoutumiskoulutus.koealusta.scheduling.import.schedule}")
    lateinit var koealustaImportSchedule: String

    @Bean
    fun dailyImportKotoSuoritukset(koealustaService: KoealustaService): Task<Instant> =
        tracer.recurringStatefulTask(
            "Hae kotoutumiskoulutuksen kielitaidon päättötestit",
            koealustaImportSchedule,
            Instant.EPOCH,
        ) { previous ->
            koealustaService.importSuoritukset(previous)
        }
}
