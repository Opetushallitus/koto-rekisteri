package fi.oph.kitu.yki

import fi.oph.kitu.asCsv
import fi.oph.kitu.isDryRun
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.LocalDate

@Service
class YkiService(
    @Qualifier("ykiRestClient")
    private val ykiRestClient: RestClient,
    private val repository: YkiRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(
        lastSeen: LocalDate? = null,
        dryRun: Boolean? = null,
    ) {
        var isDryRun = false
        val suoritukset =
            ykiRestClient
                .get()
                .uri("suoritukset")
                .exchange { _, res ->
                    isDryRun = (dryRun == true) || res.isDryRun()
                    val bodyAsString =
                        res.bodyTo(String::class.java) ?: throw RestClientException("Response body is null")
                    bodyAsString.asCsv<YkiSuoritus>()
                }

        if (suoritukset.isEmpty()) {
            logger.error("YKI responded with empty data.")
            throw RestClientException("The response is empty")
        }

        if (isDryRun) {
            logger.info("dry run complete")
            return
        }

        repository.insertSuoritukset(suoritukset)
        logger.info("suoritukset was added.")
    }
}
