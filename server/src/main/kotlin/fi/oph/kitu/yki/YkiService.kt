package fi.oph.kitu.yki

import fi.oph.kitu.asCsv
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
    ): Int {
        val suoritukset =
            ykiRestClient
                .get()
                .uri("suoritukset")
                .retrieve()
                .body(String::class.java)
                ?.asCsv<YkiSuoritus>() ?: throw RestClientException("Response body is empty")

        if (suoritukset.isEmpty()) {
            throw RestClientException("The response is empty")
        }

        if (dryRun == true) {
            return 0
        }

        val res = repository.insertSuoritukset(suoritukset)
        return res.size
    }
}
