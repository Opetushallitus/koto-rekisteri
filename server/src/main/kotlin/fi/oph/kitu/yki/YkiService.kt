package fi.oph.kitu.yki

import fi.oph.kitu.PeerService
import fi.oph.kitu.csvparsing.asCsv
import fi.oph.kitu.csvparsing.writeAsCsv
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addResponse
import fi.oph.kitu.logging.withEvent
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.SolkiSuoritusResponse
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class YkiService(
    @Qualifier("solkiRestClient")
    private val solkiRestClient: RestClient,
    private val suoritusRepository: YkiSuoritusRepository,
    private val arvioijaRepository: YkiArvioijaRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(
        from: Instant? = null,
        lastSeen: LocalDate? = null,
        dryRun: Boolean? = null,
    ): Instant? =
        logger.atInfo().withEvent("yki.importSuoritukset") { event ->
            event.add("dryRun" to dryRun, "lastSeen" to lastSeen)

            val url = if (from != null) "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}" else "suoritukset"
            val response =
                solkiRestClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .toEntity<String>()

            event.addResponse(response, PeerService.Solki)

            val suoritukset = response.body?.asCsv<SolkiSuoritusResponse>() ?: listOf()

            if (dryRun != true) {
                val res = suoritusRepository.saveAll(suoritukset.map { it.toEntity() })
                event.addKeyValue("importedSuorituksetSize", res.count())
            }
            return@withEvent suoritukset.maxOfOrNull { it.lastModified } ?: from
        }

    fun importYkiArvioijat(dryRun: Boolean = false) =
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
                response.body?.asCsv<SolkiArvioijaResponse>() ?: throw Error.EmptyArvioijatResponse()
            event.addKeyValue("yki.arvioijat.receivedCount", arvioijat.size)
            if (arvioijat.isEmpty()) {
                throw Error.EmptyArvioijat()
            }

            if (!dryRun) {
                val importedArvioijat = arvioijaRepository.saveAll(arvioijat.map { it.toEntity() })
                event.addKeyValue("yki.arvioijat.importedCount", importedArvioijat.count())
            }
        }

    fun generateSuorituksetCsvStream(): InputStream =
        logger.atInfo().withEvent<InputStream>("yki.getSuorituksetCsv") { event ->
            val data = suoritusRepository.findAll()
            event.add("dataCount" to data.count())

            val outputStream = PipedOutputStream()
            val inputStream = PipedInputStream(outputStream)

            outputStream.use { stream ->
                data.writeAsCsv(stream)
            }

            return@withEvent inputStream
        }

    sealed class Error(
        message: String,
    ) : Exception(message) {
        class EmptyArvioijatResponse : Error("Empty body on arvioijat response")

        class EmptyArvioijat : Error("Unexpected empty list of arvioijat")
    }
}
