package fi.oph.kitu.yki

import fi.oph.kitu.generated.model.YkiSuoritus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class YkiService(
    @Qualifier("ykiRestClient")
    private val ykiRestClient: RestClient,
    private val repository: YkiRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset() {
        val suoritus =
            ykiRestClient
                .get()
                .uri("/yki")
                .retrieve()
                .body<YkiSuoritus>()

        if (suoritus == null) {
            logger.info("No YKI suoritus found")
            return
        }

        repository.insertSuoritus(suoritus)
        logger.info("YKI Suoritus added to repository")
    }
}
