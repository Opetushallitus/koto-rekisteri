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

@Configuration
@ConditionalOnBooleanProperty(name = ["kitu.yki.scheduling.enabled"])
class YkiScheduledTasks(
    private val tracer: Tracer,
) {
    @Value("\${kitu.yki.scheduling.import.schedule}")
    lateinit var ykiImportSchedule: String

    @Value("\${kitu.yki.scheduling.importArvioijat.schedule}")
    lateinit var ykiImportArvioijatSchedule: String

    @WithSpan
    @Bean
    fun dailyImport(ykiService: YkiService): Task<Instant?> =
        Tasks
            .recurring("YKI-import", ExtendedSchedules.parse(ykiImportSchedule), Instant::class.java)
            .initialData(Instant.EPOCH)
            .executeStateful { taskInstance, _ ->
                tracer
                    .spanBuilder("YkiScheduledTasks.dailyImport.tasks.executeStateful")
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "YKI-import")
                        ykiService.importYkiSuoritukset(taskInstance.data)
                    }
            }

    @WithSpan
    @Bean
    fun arvioijatImport(ykiService: YkiService): Task<Void> =
        Tasks
            .recurring("YKI-import-arvioijat", ExtendedSchedules.parse(ykiImportArvioijatSchedule))
            .execute { _, _ ->
                tracer
                    .spanBuilder("YkiScheduledTasks.arvioijatImport.tasks.execute")
                    .startSpan()
                    .use { span ->
                        span.setAttribute("task.name", "YKI-import-arvioijat")
                        ykiService.importYkiArvioijat()
                    }
            }
}
