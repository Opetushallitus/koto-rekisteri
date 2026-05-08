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
    @Value("\${kitu.kotoutumiskoulutus.koealusta.scheduling.importTehtavapankki.schedule}")
    var tehtavapankkiImportSchedule: String? = null

    @Bean
    fun importKotoTehtavapankki(
        tehtavapankkiService: TehtavapankkiService,
        ingestService: TehtavapankkiIngestService,
    ): Task<Void> =
        tracer.recurringTask(
            "Kotoutumiskoulutuksen kielitaidon tehtäväpankin lataus",
            tehtavapankkiImportSchedule!!,
        ) {
            tehtavapankkiService.importTehtavapankki()
            // Tuoreelle siirrolle ei tule uutta avainta jos sisältö on
            // ennallaan: poistetaan kunkin kurssin sisällä saman sisältöiset
            // objektit jotta bucket ei kasva turhaan.
            tehtavapankkiService.removeDuplicates()
            // Käydään kunkin kurssin uusimmat XML:t läpi: puretaan upotetut
            // <file>-blobit erillisiksi S3-objekteiksi (mp3-/png-assetit) ja
            // tallennetaan parsittu sisältö yleiseen tehtäväpankki-skeemaan.
            tehtavapankkiService
                .listTehtavapaketit()
                .values
                .mapNotNull { it.firstOrNull() }
                .forEach {
                    tehtavapankkiService.extractAndUploadAssets(it.key)
                    ingestService.ingestFromS3(it.key)
                }
        }
}
