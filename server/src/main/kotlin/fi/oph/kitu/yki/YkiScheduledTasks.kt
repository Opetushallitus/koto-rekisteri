package fi.oph.kitu.yki

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.util.scheduling.recurringStatefulTask
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
        tracer.recurringStatefulTask("Tarkista poikkeamat YKI-suorituksissa", ykiMonthlyImportSchedule) {
            ykiService.checkYkiAnomalies(Instant.now().minusSeconds(365.days.inWholeSeconds))
        }
}
