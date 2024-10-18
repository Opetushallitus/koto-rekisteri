package fi.oph.kitu.yki

import fi.oph.kitu.addResponse
import fi.oph.kitu.asCsv
import fi.oph.kitu.yki.responses.YkiSuoritusResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.toEntity
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
        logger
            .atInfo()
            .addKeyValue("dryRun", dryRun)
            .addKeyValue("lastSeen", lastSeen)
            .log("start yki import")

        try {
            var response =
                ykiRestClient
                    .get()
                    .uri("suoritukset")
                    .retrieve()
                    .toEntity<String>()

            logger
                .atInfo()
                .addResponse(response)
                .addKeyValue("external-system", "YKI")
                .log("YKI suoritukset response")

            val suoritukset =
                response.body?.asCsv<YkiSuoritusResponse>() ?: throw RestClientException("Response body is empty")

            if (suoritukset.isEmpty()) {
                throw RestClientException("The response is empty")
            }

            val event = logger.atInfo()

            if (dryRun != true) {
                val res = repository.saveAll(suoritukset.map { it.toEntity() })
                event.addKeyValue("importedSuorituksetSize", res.count())
            }

            event
                .addKeyValue("success", true)
                .setMessage("import done successfully")
        } catch (ex: Exception) {
            logger
                .atInfo()
                .addKeyValue("succcess", false)
                .setCause(ex)
                .setMessage("import failed")
        }
    }
}
