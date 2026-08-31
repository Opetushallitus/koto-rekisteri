package fi.oph.kitu.yki.arvioijat

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.util.scheduling.recurringTask
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YkiArvioijaScheduledTasks(
    private val tracer: Tracer,
) {
    @Value($$"${kitu.yki.scheduling.poistaVanhentuneetArvioijat.schedule}")
    lateinit var sailytysaikaSchedule: String

    @WithSpan
    @Bean
    fun poistaVanhentuneetArvioijat(arvioijaService: YkiArvioijaService): Task<Void> =
        tracer.recurringTask(
            "Poista sailytysajan ylittaneet YKI-arvioijamerkinnat",
            sailytysaikaSchedule,
        ) {
            arvioijaService.poistaSailytysajanYlittaneet()
        }
}
