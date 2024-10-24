package fi.oph.kitu.yki

import fi.oph.kitu.ExternalSystem
import fi.oph.kitu.csvparsing.asCsv
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addExternalSystem
import fi.oph.kitu.logging.addIsDuplicateKeyException
import fi.oph.kitu.logging.addResponse
import fi.oph.kitu.logging.withEvent
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
    ) = logger.atInfo().withEvent("yki.importSuoritukset") { event ->
        event
            .add("dryRun" to dryRun, "lastSeen" to lastSeen)

        val response =
            solkiRestClient
                .get()
                .uri("suoritukset")
                .retrieve()
                .toEntity<String>()

        event
            .addResponse(response)
            .addKeyValue("external-system", "solki")
                .addResponse(response)
                .addExternalSystem(ExternalSystem.Solki)

        val suoritukset =
            response.body?.asCsv<SolkiSuoritusResponse>() ?: throw RestClientException("Response body is empty")

        if (suoritukset.isEmpty()) {
            throw RestClientException("The response is empty")
        }

        if (dryRun != true) {
            val res = repository.saveAll(suoritukset.map { it.toEntity() })
            event.addKeyValue("importedSuorituksetSize", res.count())
        }
    }
}
