package fi.oph.kitu.yki

import fi.oph.kitu.addIsDuplicateKeyException
import fi.oph.kitu.addResponse
import fi.oph.kitu.csvparsing.asCsv
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
    @Qualifier("solkiRestClient")
    private val solkiRestClient: RestClient,
    private val repository: YkiRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(
        lastSeen: LocalDate? = null,
        dryRun: Boolean? = null,
    ) {
        val event = logger.atInfo()
        event
            .addKeyValue("dryRun", dryRun)
            .addKeyValue("lastSeen", lastSeen)

        try {
            val response =
                solkiRestClient
                    .get()
                    .uri("suoritukset")
                    .retrieve()
                    .toEntity<String>()

            event
                .addResponse(response)
                .addKeyValue("external-system", "solki")

            val suoritukset =
                response.body?.asCsv<SolkiSuoritusResponse>() ?: throw RestClientException("Response body is empty")

            if (suoritukset.isEmpty()) {
                throw RestClientException("The response is empty")
            }

            if (dryRun != true) {
                val res = repository.saveAll(suoritukset.map { it.toEntity() })
                event.addKeyValue("importedSuorituksetSize", res.count())
            }

            event
                .addKeyValue("success", true)
                .setMessage("import done successfully")
        } catch (ex: Exception) {
            event
                .addKeyValue("succcess", false)
                .addIsDuplicateKeyException(ex)
                .setCause(ex)
                .setMessage("import failed")
            throw ex
        } finally {
            event.log()
        }
    }
}
