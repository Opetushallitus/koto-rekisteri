package fi.oph.kitu.yki

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.util.scheduling.recurringStatefulTask
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

private val HELSINKI = ZoneId.of("Europe/Helsinki")

fun anomalyCheckLookback(today: LocalDate): Period =
    when {
        today.dayOfWeek == DayOfWeek.SATURDAY -> Period.ofYears(1)
        else -> Period.ofMonths(1)
    }

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.yki.scheduling.enabled"])
class YkiScheduledTasks(
    private val tracer: Tracer,
) {
    @Value($$"${kitu.yki.scheduling.import.schedule}")
    lateinit var ykiImportSchedule: String

    @Value($$"${kitu.yki.scheduling.anomalyCheck.schedule}")
    lateinit var ykiAnomalyCheckSchedule: String

    @Value($$"${kitu.yki.scheduling.anomalyCheckFullYear.schedule}")
    lateinit var ykiAnomalyCheckFullYearSchedule: String

    @Value($$"${kitu.yki.scheduling.importArvioijat.schedule}")
    lateinit var ykiImportArvioijatSchedule: String

    @WithSpan
    @Bean
    fun dailyAnomalyCheck(ykiService: YkiService): Task<Void> =
        tracer.recurringStatefulTask("Tarkista poikkeamat YKI-suorituksissa", ykiAnomalyCheckSchedule) {
            val today = LocalDate.now(HELSINKI)
            val from = today.minus(anomalyCheckLookback(today)).atStartOfDay(HELSINKI).toInstant()
            ykiService.checkYkiAnomalies(from)
        }

    @WithSpan
    @Bean
    fun onDemandFullYearAnomalyCheck(ykiService: YkiService): Task<Void> =
        tracer.recurringStatefulTask(
            "Tarkista poikkeamat YKI-suorituksissa (koko vuosi, manuaalinen)",
            ykiAnomalyCheckFullYearSchedule,
        ) {
            val from =
                LocalDate
                    .now(HELSINKI)
                    .minusYears(1)
                    .atStartOfDay(HELSINKI)
                    .toInstant()
            ykiService.checkYkiAnomalies(from)
        }
}
