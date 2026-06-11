package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import com.github.kagkarlsson.scheduler.task.Task
import fi.oph.kitu.util.scheduling.recurringTask
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!ci & !e2e & !test")
@ConditionalOnBooleanProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"])
class TehtavapankkiScheduledTasks(
    private val tracer: Tracer,
) {
    @Value($$"${kitu.kotoutumiskoulutus.koealusta.scheduling.importTehtavapankki.schedule}")
    var tehtavapankkiImportSchedule: String? = null

    @Bean
    fun importKotoTehtavapankki(importService: TehtavapankkiImportService): Task<Void> =
        tracer.recurringTask(
            "Kotoutumiskoulutuksen kielitaidon tehtäväpankin lataus",
            tehtavapankkiImportSchedule!!,
        ) {
            importService.importAndIngest()
        }
}
