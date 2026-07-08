package fi.oph.kitu.i18n

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.config.ConditionalOnNonEmptyProperty
import fi.oph.kitu.util.scheduling.recurringTask
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.lokalisointi.scheduling.enabled"])
@ConditionalOnNonEmptyProperty("kitu.lokalisointi.namespace")
class LokalisointiScheduledTasks(
    private val tracer: Tracer,
) {
    @Value($$"${kitu.lokalisointi.scheduling.refresh.schedule}")
    lateinit var refreshSchedule: String

    @Bean
    fun refreshTranslations(loader: LokalisointiLoader): Task<Void> =
        tracer.recurringTask("Päivitä käännökset lokalisointipalvelusta", refreshSchedule) {
            loader.refresh()
        }
}
