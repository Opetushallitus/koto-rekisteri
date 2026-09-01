package fi.oph.kitu.yki.arvioijat.solki

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.util.scheduling.recurringTask
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SolkiArvioijaScheduledTasks(
    private val tracer: Tracer,
) {
    @Value($$"${kitu.yki.scheduling.lahetaArvioijatSolkiin.schedule}")
    lateinit var lahetysSchedule: String

    @Value($$"${kitu.yki.scheduling.lahetaEpaonnistuneetArvioijatSolkiin.schedule}")
    lateinit var yollinenSchedule: String

    /** "3 kertaa": nopeat uusinnat vain riveille joilla yrityksia on viela jaljella. */
    @WithSpan
    @Bean
    fun lahetaArvioijatSolkiin(solki: SolkiArvioijaService): Task<Void> =
        tracer.recurringTask("Laheta YKI-arvioijat Solkiin", lahetysSchedule) {
            solki.lahetaLahettamattomat(maxYritykset = MAX_YRITYKSET)
        }

    /** "sen jalkeen saannollisesti": yollinen ajo ei valita yrityslaskurista. */
    @WithSpan
    @Bean
    fun lahetaEpaonnistuneetArvioijatSolkiin(solki: SolkiArvioijaService): Task<Void> =
        tracer.recurringTask("Laheta epaonnistuneet YKI-arvioijat Solkiin", yollinenSchedule) {
            solki.lahetaLahettamattomat()
        }

    companion object {
        private const val MAX_YRITYKSET = 3
    }
}
