package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.auditlogs.AuditLogger
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service

@Service
class YkiArvioijaService(
    private val repository: YkiArvioijaRepository,
    private val auditLogger: AuditLogger,
) {
    @WithSpan
    fun haeSivullinen(params: YkiArvioijaParams): List<YkiArvioijaListRow> =
        repository.findForListView(params).also { rows ->
            auditLogger.logAllInternalOnly("Yki arvioija viewed", rows) {
                arrayOf("arvioija.oid" to it.arvioijaOid.toString())
            }
        }

    @WithSpan
    fun laske(params: YkiArvioijaParams): Int = repository.countForListView(params)

    /** CSV-vientiin: koko suodatettu joukko ilman sivutusta. */
    @WithSpan
    fun haeKaikki(params: YkiArvioijaParams): List<YkiArvioijaListRow> =
        haeSivullinen(params.copy(page = 1, limit = Int.MAX_VALUE))
}
