package fi.oph.kitu.koski

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ExtendedSchedules
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["kitu.koski.scheduling.enabled"], matchIfMissing = false)
class KoskiScheduledTask(
    private val tracer: Tracer,
) {
    @Value("\${kitu.koski.scheduling.send.schedule}")
    lateinit var koskiSendSuorituksetSchedule: String

    @Bean
    fun sendSuoriukset(koskiService: KoskiService): Task<Void> =
        Tasks
            .recurring("KOSKI-send-YKI-suoritukset", ExtendedSchedules.parse(koskiSendSuorituksetSchedule))
            .execute { _, _ ->
                tracer.spanBuilder("KoskiScheduledTask.sendSuoritukset.tasks.execute").startSpan().use { span ->
                    span.setAttribute("task.name", "KOSKI-send-YKI-suoritukset")
                    koskiService.sendYkiSuorituksetToKoski()
                }
            }
}
