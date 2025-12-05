package fi.oph.kitu.ilmoittautumisjarjestelma

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
class IlmoittautumisjarjestelmaScheduledTasks(
    private val tracer: Tracer,
) {
    @Value("\${kitu.yki.scheduling.retrySendingFailedArviointitilat.schedule}")
    lateinit var retrySendingFailedArviointitilatSchedule: String

    @WithSpan
    @Bean
    fun retrySendingFailedArviointitilat(ilmoittautumisjarjestelma: IlmoittautumisjarjestelmaService): Task<Void> =
        Tasks
            .recurring("Send arviointitilat", ExtendedSchedules.parse(retrySendingFailedArviointitilatSchedule))
            .execute { _, _ ->
                tracer
                    .spanBuilder(
                        "IlmoittautumisjarjestelmaScheduledTasks.sendAllUpdatedArvioinninTilat.tasks.executeStateful",
                    ).startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "sendAllUpdatedArvioinninTilat")
                        ilmoittautumisjarjestelma.sendAllUpdatedArvioinninTilat()
                    }
            }
}
