package fi.oph.kitu.yki

import fi.oph.kitu.PeerService
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.csvparsing.addErrors
import fi.oph.kitu.csvparsing.foldWithErrors
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addHttpResponse
import fi.oph.kitu.logging.withEvent
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class YkiService(
    @Qualifier("solkiRestClient")
    private val solkiRestClient: RestClient,
    private val suoritusRepository: YkiSuoritusRepository,
    private val suoritusMapper: YkiSuoritusMappingService,
    private val arvioijaRepository: YkiArvioijaRepository,
    private val arvioijaMapper: YkiArvioijaMappingService,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.yki.import.suoritukset.ontinue-on-error}")
    private var importSuorituksetContinueOnError: Boolean = false

    fun importYkiSuoritukset(
        from: Instant? = null,
        lastSeen: LocalDate? = null,
        dryRun: Boolean? = null,
    ): Instant? =
        logger.atInfo().withEvent("yki.importSuoritukset") { event ->
            val parser = CsvParser(event)
            event.add("dryRun" to dryRun, "lastSeen" to lastSeen)

            val url = if (from != null) "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}" else "suoritukset"
            val response =
                solkiRestClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .toEntity<String>()

            event.addHttpResponse(PeerService.Solki, "suoritukset", response)

            val suoritukset = mutableListOf<YkiSuoritusCsv>()
            parser
                .convertCsvtToResults<YkiSuoritusCsv>(response.body ?: "")
                .foldWithErrors(importSuorituksetContinueOnError) {
                    event.addErrors(it)
                }

            if (dryRun != true) {
                val res = suoritusRepository.saveAll(suoritusMapper.convertToEntityIterable(suoritukset))
                event.add("importedSuorituksetSize" to res.count())
            }
            return@withEvent suoritukset.maxOfOrNull { it.lastModified } ?: from
        }

    fun importYkiArvioijat(dryRun: Boolean = false) =
        logger.atInfo().withEvent("yki.importArvioijat") { event ->
            val parser = CsvParser(event)
            val response =
                solkiRestClient
                    .get()
                    .uri("arvioijat")
                    .retrieve()
                    .toEntity<String>()

            event.addHttpResponse(PeerService.Solki, "arvioijat", response)

            val arvioijat =
                parser
                    .convertCsvtToResults<SolkiArvioijaResponse>(
                        response.body ?: throw Error.EmptyArvioijatResponse(),
                    ).foldWithErrors(false) {
                        event.addErrors(it)
                    }

            event.add("yki.arvioijat.receivedCount" to arvioijat.size)
            if (arvioijat.isEmpty()) {
                throw Error.EmptyArvioijat()
            }

            if (!dryRun) {
                val importedArvioijat = arvioijaRepository.saveAll(arvioijaMapper.convertToEntityIterable(arvioijat))
                event.add("yki.arvioijat.importedCount" to importedArvioijat.count())
            }
        }

    fun generateSuorituksetCsvStream(includeVersionHistory: Boolean): ByteArrayOutputStream =
        logger.atInfo().withEvent("yki.getSuorituksetCsv") { event ->
            val parser = CsvParser(event, useHeader = true)
            val data =
                if (includeVersionHistory) {
                    suoritusRepository.findAllOrdered()
                } else {
                    suoritusRepository.findAllDistinct()
                }
            event.add("dataCount" to data.count())
            val writableData = suoritusMapper.convertToResponseIterable(data)
            val outputStream = ByteArrayOutputStream()
            parser.streamDataAsCsv(outputStream, writableData)

            return@withEvent outputStream
        }

    sealed class Error(
        message: String,
    ) : Exception(message) {
        class EmptyArvioijatResponse : Error("Empty body on arvioijat response")

        class EmptyArvioijat : Error("Unexpected empty list of arvioijat")
    }
}
