package fi.oph.kitu.yki

import fi.oph.kitu.PeerService
import fi.oph.kitu.SortDirection
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addHttpResponse
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorService
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
    private val suoritusErrorService: YkiSuoritusErrorService,
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

                val (suoritukset, errors) = parser.safeConvertCsvToData<YkiSuoritusCsv>(response.body ?: "")
                if (errors.isNotEmpty()) {
                    suoritusErrorService.saveErrors(errors)
                }

                event.add("yki.suoritukset.receivedCount" to suoritukset.size)

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

                val (arvioijat) =
                    parser.safeConvertCsvToData<SolkiArvioijaResponse>(
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

    fun allSuoritukset(versionHistory: Boolean): List<YkiSuoritusEntity> =
        suoritusRepository
            .find(distinct = !versionHistory)
            .toList()
            .also {
                auditLogger.logAll("Yki suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                    )
                }
            }

    fun countSuoritukset(
        searchBy: String = "",
        versionHistory: Boolean = false,
    ): Long = suoritusRepository.countSuoritukset(searchBy = searchBy, distinct = !versionHistory)

    fun findSuorituksetPaged(
        searchStr: String = "",
        column: YkiSuoritusColumn = YkiSuoritusColumn.Tutkintopaiva,
        direction: SortDirection,
        versionHistory: Boolean = false,
        limit: Int,
        offset: Int,
    ): List<YkiSuoritusEntity> =
        suoritusRepository
            .find(
                searchBy = searchStr,
                column = column,
                direction = direction,
                distinct = !versionHistory,
                limit = limit,
                offset = offset,
            ).toList()
            .also {
                auditLogger.logAll("Yki suoritus viewed", it) { suoritus ->
                    arrayOf(
                        "suoritus.id" to suoritus.id,
                    )
                }
            }

    fun allArvioijat(
        orderBy: YkiArvioijaColumn = YkiArvioijaColumn.Rekisteriintuontiaika,
        orderByDirection: SortDirection = SortDirection.DESC,
    ): List<YkiArvioijaEntity> =
        arvioijaRepository
            .findAllSorted(orderBy.entityName, orderByDirection)
            .toList()
            .also {
                auditLogger.logAll("Yki arvioija viewed", it) { arvioija ->
                    arrayOf("arvioija.oppijanumero" to arvioija.arvioijanOppijanumero)
                }
            }

    sealed class Error(
        message: String,
    ) : Throwable(message) {
        class EmptyArvioijatResponse : Error("Empty body on arvioijat response")

        class EmptyArvioijat : Error("Unexpected empty list of arvioijat")
    }
}
