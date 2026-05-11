package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.tehtavapankki.TehtavapankkiRepository
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * TODO(tilapäinen): kertaluonteinen tarkistus käynnistyksen yhteydessä.
 * Käy läpi S3:n uusimmat tehtäväpaketit ja varmistaa että jokainen löytyy
 * tietokannasta ja sen embeddatut tiedostot on purettu S3:een. Puuttuvat
 * täydennetään samoilla operaatioilla joita ajastettu importti käyttää.
 * Voidaan poistaa kun olemassaolevat ympäristöt on käyty kerran läpi.
 */
@Component
@Profile("!ci & !e2e & !test")
@ConditionalOnBooleanProperty(name = ["kitu.kotoutumiskoulutus.koealusta.scheduling.enabled"])
class TehtavapankkiStartupCheck(
    private val tehtavapankkiService: TehtavapankkiService,
    private val ingestService: TehtavapankkiIngestService,
    private val repository: TehtavapankkiRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @WithSpan
    fun checkAndFix() {
        val latestPerCourse =
            tehtavapankkiService
                .listTehtavapaketit()
                .values
                .mapNotNull { it.firstOrNull() }
        if (latestPerCourse.isEmpty()) return

        val knownInDb = repository.findIdsByS3Avain(latestPerCourse.map { it.key }).keys

        latestPerCourse.forEach { obj ->
            val xmlKey = obj.key
            try {
                if (!tehtavapankkiService.hasExtractedAssets(xmlKey)) {
                    logger.info("Tehtäväpaketin $xmlKey assetit puuttuvat S3:sta — puretaan.")
                    tehtavapankkiService.extractAndUploadAssets(xmlKey)
                }
                if (xmlKey !in knownInDb) {
                    logger.info("Tehtäväpaketti $xmlKey puuttuu tietokannasta — ingestoidaan.")
                    ingestService.ingestFromS3(xmlKey)
                }
            } catch (e: Exception) {
                logger.warn("Tehtäväpaketin $xmlKey käynnistystarkistus epäonnistui", e)
            }
        }
    }
}
