package fi.oph.kitu.yki

import fi.oph.kitu.csvparsing.CsvArgs
import fi.oph.kitu.csvparsing.asCsv
import fi.oph.kitu.csvparsing.writeAsCsv
import fi.oph.kitu.yki.arvioijat.SolkiArvioijaResponse
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
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
    private val arvioijaRepository: YkiArvioijaRepository,
) {
    @WithSpan
    fun importYkiSuoritukset(
        from: Instant? = null,
        dryRun: Boolean? = null,
    ): Instant? {
        val span = Span.current()

        val url = if (from != null) "suoritukset?m=${DateTimeFormatter.ISO_INSTANT.format(from)}" else "suoritukset"
        span.setAttribute("url", url)

        val response =
            solkiRestClient
                .get()
                .uri(url)
                .retrieve()
                .toEntity<String>()

        val suoritukset = response.body?.asCsv<YkiSuoritusCsv>() ?: listOf()

        if (dryRun != true) {
            val res = suoritusRepository.saveAll(suoritukset.map { it.toEntity() })
        }

        return suoritukset.maxOfOrNull { it.lastModified } ?: from
    }

    @WithSpan
    fun importYkiArvioijat(dryRun: Boolean = false) {
        val span = Span.current()

        val response =
            solkiRestClient
                .get()
                .uri("arvioijat")
                .retrieve()
                .toEntity<String>()

        val arvioijat =
            response.body?.asCsv<SolkiArvioijaResponse>(CsvArgs())
                ?: throw Error.EmptyArvioijatResponse()

        span.setAttribute("yki.arvioijat.receivedCount", arvioijat.size.toLong())

        if (arvioijat.isEmpty()) {
            throw Error.EmptyArvioijat()
        }

        if (dryRun) {
            return
        }

        arvioijaRepository.saveAll(arvioijat.map { it.toEntity() })
    }

    fun generateSuorituksetCsvStream(includeVersionHistory: Boolean): ByteArrayOutputStream {
        val data = if (includeVersionHistory) suoritusRepository.findAll() else suoritusRepository.findAllDistinct()
        Span.current().setAttribute("suoritus.count", data.count().toLong())
        val writableData = data.map { it.toYkiSuoritusCsv() }
        val outputStream = ByteArrayOutputStream()
        writableData.writeAsCsv(outputStream, CsvArgs(useHeader = true))
        return outputStream
    }

    sealed class Error(
        message: String,
    ) : Exception(message) {
        class EmptyArvioijatResponse : Error("Empty body on arvioijat response")

        class EmptyArvioijat : Error("Unexpected empty list of arvioijat")
    }
}
