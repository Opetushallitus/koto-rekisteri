package fi.oph.kitu.yki

import fi.oph.kitu.PeerService
import fi.oph.kitu.csvparsing.CsvParser
import fi.oph.kitu.logging.Logging
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addUser
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaMappingService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusMappingService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.io.ByteArrayOutputStream
import java.time.Instant
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
    private val auditLogger: Logger = Logging.auditLogger()

    @WithSpan
    fun importYkiSuoritukset(
        from: Instant? = null,
        dryRun: Boolean? = null,
    ): Instant? {
        val parser = CsvParser()

        val span = Span.current()

        val url = if (from != null) "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}" else "suoritukset"
        span.setAttribute("url", url)

        val response =
            solkiRestClient
                .get()
                .uri(url)
                .retrieve()
                .toEntity<String>()

        val suoritukset =
            parser
                .convertCsvToData<YkiSuoritusCsv>(response.body ?: "")
                .also {
                    for (suoritus in it) {
                        auditLogger
                            .atInfo()
                            .add(
                                "principal" to "yki.importSuoritukset",
                                "suoritus.id" to suoritus.suoritusID,
                            )
                    }
                }

        if (dryRun != true) {
            suoritusRepository.saveAll(suoritusMapper.convertToEntityIterable(suoritukset))
        }

        return suoritukset.maxOfOrNull { it.lastModified } ?: from
    }

    @WithSpan
    fun importYkiArvioijat(dryRun: Boolean = false) {
        val parser = CsvParser()

        val span = Span.current()

        val response =
            solkiRestClient
                .get()
                .uri("arvioijat")
                .retrieve()
                .toEntity<String>()

        val arvioijat =
            parser.convertCsvToData<SolkiArvioijaResponse>(
                response.body ?: throw Error.EmptyArvioijatResponse(),
            )

        span.setAttribute("yki.arvioijat.receivedCount", arvioijat.size.toLong())

        if (arvioijat.isEmpty()) {
            throw Error.EmptyArvioijat()
        }

        if (dryRun) {
            return
        }

        val importedArvioijat = arvioijaRepository.saveAll(arvioijaMapper.convertToEntityIterable(arvioijat))

        for (arvioija in importedArvioijat) {
            auditLogger
                .atInfo()
                .add(
                    "principal" to "yki.importArvioijat",
                    "peer.service" to PeerService.Solki.value,
                    "arvioija.oppijanumero" to arvioija.arvioijanOppijanumero,
                ).log("YKI arvioija imported")
        }
    }

    fun generateSuorituksetCsvStream(includeVersionHistory: Boolean): ByteArrayOutputStream {
        val parser = CsvParser(useHeader = true)
        val suoritukset = allSuoritukset(includeVersionHistory)
        Span.current().setAttribute("suoritus.count", suoritukset.count().toLong())
        val writableData = suoritusMapper.convertToResponseIterable(suoritukset)
        val outputStream = ByteArrayOutputStream()
        parser.streamDataAsCsv(outputStream, writableData)
        return outputStream
    }

    fun allSuoritukset(versionHistory: Boolean?): List<YkiSuoritusEntity> =
        if (versionHistory == true) {
            suoritusRepository.findAllOrdered().toList()
        } else {
            suoritusRepository.findAllDistinct().toList()
        }.also {
            for (suoritus in it) {
                auditLogger
                    .atInfo()
                    .addUser()
                    .add(
                        "suoritus.id" to suoritus.id,
                    ).log("Yki suoritus viewed")
            }
        }

    fun allArvioijat(): List<YkiArvioijaEntity> =
        arvioijaRepository.findAll().toList().also {
            for (arvioija in it) {
                auditLogger
                    .atInfo()
                    .addUser()
                    .add(
                        "arvioija.oppijanumero" to arvioija.arvioijanOppijanumero,
                    ).log("Yki arvioija viewed")
            }
        }

    sealed class Error(
        message: String,
    ) : Throwable(message) {
        class EmptyArvioijatResponse : Error("Empty body on arvioijat response")

        class EmptyArvioijat : Error("Unexpected empty list of arvioijat")
    }
}
