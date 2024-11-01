package fi.oph.kitu.yki

import fi.oph.kitu.PeerService
import fi.oph.kitu.csvparsing.asCsv
import fi.oph.kitu.logging.add
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
    private val arvioijaRepository: YkiArvioijaRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(
        lastSeen: LocalDate? = null,
        dryRun: Boolean? = null,
    ) = logger.atInfo().withEvent("yki.importSuoritukset") { event ->
        event.add("dryRun" to dryRun, "lastSeen" to lastSeen)

        val response =
            solkiRestClient
                .get()
                .uri("suoritukset")
                .retrieve()
                .toEntity<String>()

        event.addResponse(response, PeerService.Solki)

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

    /**
     * Runs the task of importing all arvioijat from YKI.
     *
     * @return `true` if import was completed successfully, otherwise `false`.
     */
    fun importYkiArvioijat(dryRun: Boolean = false): Boolean =
        logger.atInfo().withEvent("yki.importArvioijat") { event ->
            val response =
                solkiRestClient
                    .get()
                    .uri("arvioijat")
                    .retrieve()
                    .toEntity<String>()

            event
                .addResponse("yki.arvioijat.get", response)
                .addKeyValue("peer.service", PeerService.Solki.value)

            val arvioijat =
                response.body?.asCsv<SolkiArvioijaResponse>() ?: listOf()
            if (arvioijat.isEmpty()) {
                event
                    .addKeyValue("success", false)
                    .addKeyValue("yki.arvioijat.receivedCount", 0)
                    .setMessage("import failed: unexpected empty get arvioijat response")
                return@withEvent false
            }

            if (!dryRun) {
                val res = arvioijaRepository.saveAll(arvioijat.map { it.toEntity() })
                event.addKeyValue("yki.arvioijat.importedCount", res.count())
            }

            event
                .addKeyValue("success", true)
                .setMessage("import done successfully")
            return@withEvent true
        }
}
