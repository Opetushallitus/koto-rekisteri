package fi.oph.kitu.yki

import fi.oph.kitu.csvBody
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

    fun importYkiSuoritukset(lastSeen: LocalDate? = null) {
        val dto =
            ykiRestClient
                .get()
                .uri("suoritukset")
                .retrieve()
                .csvBody<YkiSuoritusResponse>()

        if (dto.isEmpty()) {
            logger.error("YKI reponded with empty data.")
            throw RestClientException("The response is empty")
        }

        val suoritukset = dto.map { it.toYkiSuoritus() }
        repository.insertSuoritukset(suoritukset)
        logger.info("suoritukset was added.")
    }
}
