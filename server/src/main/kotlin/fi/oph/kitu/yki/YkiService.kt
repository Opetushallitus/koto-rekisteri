package fi.oph.kitu.yki

import fi.oph.kitu.PeerService
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addHttpResponse
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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
    private val auditLogger: AuditLogger,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(
        from: Instant? = null,
        lastSeen: LocalDate? = null,
        dryRun: Boolean? = null,
    ): Instant? =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                val parser = CsvParser(event)
                event.add("dryRun" to dryRun, "lastSeen" to lastSeen)

                val url =
                    if (from != null) {
                        "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}"
                    } else {
                        "suoritukset"
                    }
                val response =
                    solkiRestClient
                        .get()
                        .uri(url)
                        .retrieve()
                        .toEntity<String>()

                event.addHttpResponse(PeerService.Solki, "suoritukset", response)

                val suoritukset =
                    parser
                        .convertCsvToData<YkiSuoritusCsv>(response.body ?: "")

                if (dryRun != true) {
                    val saved = suoritusRepository.saveAll(suoritusMapper.convertToEntityIterable(suoritukset))
                    event.add("importedSuorituksetSize" to saved.count())
                    auditLogger.logAll("YKI suoritus imported", saved) { suoritus ->
                        arrayOf(
                            "principal" to "yki.importSuoritukset",
                            "suoritus.id" to suoritus.suoritusId,
                        )
                    }
                }
                return@withEventAndPerformanceCheck suoritukset.maxOfOrNull { it.lastModified } ?: from
            }.apply {
                addDefaults("yki.importSuoritukset")
                addDatabaseLogs()
            }.getOrThrow()

    fun importYkiArvioijat(dryRun: Boolean = false) =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                val parser = CsvParser(event)
                val response =
                    solkiRestClient
                        .get()
                        .uri("arvioijat")
                        .retrieve()
                        .toEntity<String>()

                event.addHttpResponse(PeerService.Solki, "arvioijat", response)

                val arvioijat =
                    parser.convertCsvToData<SolkiArvioijaResponse>(
                        response.body ?: throw Error.EmptyArvioijatResponse(),
                    )

                event.add("yki.arvioijat.receivedCount" to arvioijat.size)

                if (arvioijat.isEmpty()) {
                    throw Error.EmptyArvioijat()
                }

                if (!dryRun) {
                    val importedArvioijat =
                        arvioijaRepository.saveAll(
                            arvioijaMapper.convertToEntityIterable(arvioijat),
                        )
                    event.add("yki.arvioijat.importedCount" to importedArvioijat.count())

                    auditLogger.logAll("YKI arvioija imported", importedArvioijat) { arvioija ->
                        arrayOf(
                            "principal" to "yki.importArvioijat",
                            "peer.service" to PeerService.Solki.value,
                            "arvioija.oppijanumero" to arvioija.arvioijanOppijanumero,
                        )
                    }
                }
            }.apply {
                addDefaults("yki.importArvioijat")
                addDatabaseLogs()
            }.getOrThrow()

    fun generateSuorituksetCsvStream(includeVersionHistory: Boolean): ByteArrayOutputStream =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                val parser = CsvParser(event, useHeader = true)
                val suoritukset = allSuoritukset(includeVersionHistory)
                event.add("dataCount" to suoritukset.count())
                val writableData = suoritusMapper.convertToResponseIterable(suoritukset)
                val outputStream = ByteArrayOutputStream()
                parser.streamDataAsCsv(outputStream, writableData)

                return@withEventAndPerformanceCheck outputStream
            }.apply {
                addDefaults("yki.getSuorituksetCsv")
                addDatabaseLogs()
            }.getOrThrow()

    fun allSuoritukset(versionHistory: Boolean?): List<YkiSuoritusEntity> =
        if (versionHistory == true) {
            suoritusRepository.findAllOrdered().toList()
        } else {
            suoritusRepository.findAllDistinct().toList()
        }.also {
            auditLogger.logAll("Yki suoritus viewed", it) { suoritus ->
                arrayOf(
                    "suoritus.id" to suoritus.id,
                )
            }
        }

    fun allArvioijat(): List<YkiArvioijaEntity> =
        arvioijaRepository.findAll().toList().also {
            auditLogger.logAll("Yki arvioija viewed", it) { arvioija ->
                arrayOf(
                    "arvioija.oppijanumero" to arvioija.arvioijanOppijanumero,
                )
            }
        }

    sealed class Error(
        message: String,
    ) : Throwable(message) {
        class EmptyArvioijatResponse : Error("Empty body on arvioijat response")

        class EmptyArvioijat : Error("Unexpected empty list of arvioijat")
    }
}
