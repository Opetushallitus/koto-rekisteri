package fi.oph.kitu.yki

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.kitu.ExtendedSchedules
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
@ConditionalOnProperty(name = ["kitu.yki.scheduling.enabled"], matchIfMissing = false)
class YkiScheduledTasks {
    @Value("\${kitu.yki.scheduling.import.schedule}")
    lateinit var ykiImportSchedule: String

    @Value("\${kitu.yki.scheduling.importArvioijat.schedule}")
    lateinit var ykiImportArvioijatSchedule: String

    @Bean
    fun dailyImport(ykiService: YkiService): Task<Instant?> =
        Tasks
            .recurring("YKI-import", ExtendedSchedules.parse(ykiImportSchedule), Instant::class.java)
            .initialData(Instant.EPOCH)
            .executeStateful { taskInstance, _ -> ykiService.importYkiSuoritukset(taskInstance.data ?: Instant.now()) }

    @Bean
    fun arvioijatImport(ykiService: YkiService): Task<Void> =
        Tasks
            .recurring("YKI-import-arvioijat", ExtendedSchedules.parse(ykiImportArvioijatSchedule))
            .execute { _, _ -> ykiService.importYkiArvioijat() }
}
