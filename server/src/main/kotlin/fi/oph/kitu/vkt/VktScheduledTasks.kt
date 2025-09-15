package fi.oph.kitu.vkt

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ExtendedSchedules
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VktScheduledTasks(
    private val tracer: Tracer,
) {
    @Value("\${kitu.vkt.scheduling.cleanup.schedule}")
    lateinit var vktCleanupSchedule: String

    @WithSpan
    @Bean
    fun cleanup(vktService: VktSuoritusService): Task<Void> =
        Tasks
            .recurring("VKT-cleanup", ExtendedSchedules.parse(vktCleanupSchedule))
            .execute { _, _ ->
                tracer
                    .spanBuilder("VktScheduledTasks.cleanup.tasks.executeStateful")
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "VKT-cleanup")
                        vktService.cleanup()
                    }
            }
}
