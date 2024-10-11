package fi.oph.kitu.yki

import fi.oph.kitu.csvBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class YkiService(
    @Qualifier("ykiRestClient")
    private val ykiRestClient: RestClient,
    private val repository: YkiRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset() {
        val suoritukset =
            ykiRestClient
                .get()
                .uri("/yki")
                .retrieve()
                .csvBody()
                ?.map { Mappers.toYkiSuoritus(it) }

        if (suoritukset == null) {
            logger.info("No YKI suoritukset found")
            return
        }

        repository.insertSuoritukset(suoritukset)
        logger.info("YKI Suoritukset was added to repository")
    }
}
