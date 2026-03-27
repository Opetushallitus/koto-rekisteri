package fi.oph.kitu.koski

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ConditionalOnNonEmptyProperty
import fi.oph.kitu.ExtendedSchedules
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.koski.scheduling.enabled"])
@ConditionalOnNonEmptyProperty("kitu.koski.scheduling.yki.schedule")
class KoskiYkiScheduledTask(
    private val tracer: Tracer,
) {
    @Value("\${kitu.koski.scheduling.yki.schedule}")
    lateinit var ykiSchedule: String

    @Bean
    fun sendYkiSuoritukset(koskiService: KoskiService): Task<String?> =
        Tasks
            .recurring(
                "KOSKI-send-YKI-suoritukset",
                ExtendedSchedules.parse(ykiSchedule),
                String::class.java,
            ).executeStateful { _, _ ->
                tracer
                    .spanBuilder("KoskiScheduledTask.sendSuoritukset.tasks.execute")
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "KOSKI-send-YKI-suoritukset")
                        koskiService.sendYkiSuorituksetToKoski().toString()
                    }
            }
}

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.koski.scheduling.enabled"])
@ConditionalOnNonEmptyProperty("kitu.koski.scheduling.vkt.schedule")
class KoskiVktScheduledTask(
    private val tracer: Tracer,
) {
    @Value("\${kitu.koski.scheduling.vkt.schedule}")
    lateinit var vktSchedule: String

    @Bean
    fun sendVktSuoritukset(koskiService: KoskiService): Task<String?> =
        Tasks
            .recurring(
                "KOSKI-send-VKT-suoritukset",
                ExtendedSchedules.parse(vktSchedule),
                String::class.java,
            ).executeStateful { _, _ ->
                tracer
                    .spanBuilder("KoskiScheduledTask.sendSuoritukset.tasks.execute")
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "KOSKI-send-VKT-suoritukset")
                        koskiService.sendVktSuorituksetToKoski().toString()
                    }
            }
}
