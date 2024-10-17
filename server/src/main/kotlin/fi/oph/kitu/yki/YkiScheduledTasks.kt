package fi.oph.kitu.yki

import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.math.log

@Configuration
@ConditionalOnProperty(name = ["kitu.yki.scheduling.enabled"], matchIfMissing = false)
class YkiScheduledTasks {
    @Value("\${kitu.yki.scheduling.import.schedule}")
    lateinit var ykiImportSchedule: String

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val dryRun: Boolean? = false

    @Bean
    fun dailyImport(ykiService: YkiService): Task<Void> =
        Tasks
            .recurring("YKI-import", Schedules.parseSchedule(ykiImportSchedule))
            .execute { _, _ -> wrapLogging { ykiService.importYkiSuoritukset() } }

    fun wrapLogging(method: () -> Int) {
        logger.atTrace().log("imports started")
        try {
            val res = method()
            logger
                .atInfo()
                .addKeyValue("dryRun", dryRun)
                .addKeyValue("importSize", res)
                .log("imports done")
        } catch (e: Exception) {
            logger
                .atError()
                .addKeyValue("dryRun", dryRun)
                .setCause(e)
                .log("imports failed")
            throw e
        }
    }
}
