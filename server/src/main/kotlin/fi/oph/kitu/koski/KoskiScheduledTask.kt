package fi.oph.kitu.koski

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.config.ConditionalOnNonEmptyProperty
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.util.scheduling.recurringStatefulTask
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
    fun sendYkiSuoritukset(koskiService: KoskiService): Task<String> =
        tracer.recurringStatefulTask("Lähetä YKI-suoritukset KOSKI-palveluun", ykiSchedule, "") { _ ->
            koskiService.sendYkiSuorituksetToKoski().getOrThrow().toString()
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
    fun sendVktSuoritukset(koskiService: KoskiService): Task<String> =
        tracer.recurringStatefulTask("Lähetä VKT-suoritukset KOSKI-palveluun", vktSchedule, "") { _ ->
            koskiService.sendVktSuorituksetToKoski().getOrThrow().toString()
        }
}
