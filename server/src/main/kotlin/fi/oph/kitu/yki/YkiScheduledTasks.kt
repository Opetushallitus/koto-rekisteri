package fi.oph.kitu.yki

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ExtendedSchedules
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import kotlin.time.Duration.Companion.days

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.yki.scheduling.enabled"])
class YkiScheduledTasks(
    private val tracer: Tracer,
) {
    @Value("\${kitu.yki.scheduling.import.schedule}")
    lateinit var ykiImportSchedule: String

    @Value("\${kitu.yki.scheduling.largeImport.schedule}")
    lateinit var ykiMonthlyImportSchedule: String

    @Value("\${kitu.yki.scheduling.importArvioijat.schedule}")
    lateinit var ykiImportArvioijatSchedule: String

    @WithSpan
    @Bean
    fun monthlyCheck(ykiService: YkiService): Task<Void> =
        Tasks
            .recurring("Tarkista poikkeamat YKI-suorituksissa", ExtendedSchedules.parse(ykiMonthlyImportSchedule))
            .executeStateful { _, _ ->
                tracer
                    .spanBuilder("YkiScheduledTasks.monthlyCheck.tasks.executeStateful")
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "YKI-check-anomalies")
                        ykiService.checkYkiAnomalies(Instant.now().minusSeconds(365.days.inWholeSeconds))
                        null
                    }
            }
}
