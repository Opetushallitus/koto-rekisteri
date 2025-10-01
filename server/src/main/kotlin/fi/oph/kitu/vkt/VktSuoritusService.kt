package fi.oph.kitu.vkt

import fi.oph.kitu.Cache
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.logging.AuditLogOperation
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.vkt.CustomVktSuoritusRepository.Tutkintoryhma
import fi.oph.kitu.vkt.html.VktTableItem
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Service
class VktSuoritusService(
    private val suoritusRepository: VktSuoritusRepository,
    private val customSuoritusRepository: CustomVktSuoritusRepository,
    private val osakoeRepository: VktOsakoeRepository,
    private val auditLogger: AuditLogger,
    private val oppijanumeroService: OppijanumeroService,
) {
    @Value("\${kitu.vkt.scheduling.cleanup.retentionTime}")
    lateinit var retentionTimeForDeletedSetting: String
    val retentionTimeForDeletedSeconds by lazy { Duration.parse(retentionTimeForDeletedSetting).inWholeSeconds }

    @WithSpan("VktSuoritusService.getSuorituksetAndPagination")
    fun getSuorituksetAndPagination(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
        sortColumn: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        pageNumber: Int,
        searchQuery: String?,
    ) = Pair(
        getIlmoittautuneetForListView(taitotaso, arvioidut, sortColumn, sortDirection, pageNumber, searchQuery),
        getPagination(sortColumn, sortDirection, pageNumber, taitotaso, arvioidut, searchQuery),
    )

    @WithSpan("VktSuoritusService.getIlmoittautuneetForListView")
    fun getIlmoittautuneetForListView(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
        sortColumn: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        pageNumber: Int,
        searchQuery: String?,
    ): List<VktTableItem> =
        customSuoritusRepository.findForListView(
            taitotaso = taitotaso,
            arvioidut = arvioidut,
            column = sortColumn,
            direction = sortDirection,
            limit = PAGE_SIZE,
            offset = (pageNumber - 1) * PAGE_SIZE,
            searchQuery = searchQuery,
        )

    @WithSpan("VktSuoritusService.getPagination")
    fun getPagination(
        sortColumn: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        currentPageNumber: Int,
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
        searchQuery: String?,
    ): Pagination =
        Pagination.valueOf(
            currentPageNumber = currentPageNumber,
            numberOfRows = listRowCounts.get(Triple(taitotaso, arvioidut, searchQuery))!!,
            pageSize = PAGE_SIZE,
            url = { "?page=$it&sortColumn=$sortColumn&sortDirection=$sortDirection" },
        )

    @WithSpan("VktSuoritusService.getSuoritus")
    fun getSuoritus(id: Int): Optional<VktHenkilosuoritus> =
        suoritusRepository
            .findById(id)
            .map { it.toHenkilosuoritus() }

    @WithSpan("VktSuoritusService.getOppijanSuoritukset")
    fun getOppijanSuoritukset(
        id: Tutkintoryhma,
        includeSuorituksenVastaanottajat: Boolean = true,
    ): VktHenkilosuoritus? {
        val ids = customSuoritusRepository.getOppijanSuoritusIds(id)
        val suoritukset =
            ids
                .mapNotNull { suoritusRepository.findById(it).getOrNull() }
                .map { it.toHenkilosuoritus() }
                .also {
                    it.firstOrNull()?.henkilo?.let { henkilo ->
                        auditLogger.log(
                            operation = AuditLogOperation.VktSuoritusViewed,
                            oppijaHenkiloOid = henkilo.oid,
                        )
                    }
                }
        val suorituksenVastaanottajat =
            if (includeSuorituksenVastaanottajat) {
                suoritukset
                    .mapNotNull { it.suoritus.suorituksenVastaanottaja }
                    .toSet()
                    .associateBy({ it }, { oid ->
                        oppijanumeroService.getHenkilo(oid).getOrNull()?.kokoNimi() ?: oid.toString()
                    })
            } else {
                mapOf()
            }

        return if (suoritukset.isEmpty()) null else VktSuoritus.merge(suoritukset, suorituksenVastaanottajat)
    }

    @WithSpan("VktSuoritusService.setOsakoeArvosana")
    fun setOsakoeArvosana(
        osakoeId: Int,
        arvosana: Koodisto.VktArvosana?,
        arviointipaiva: LocalDate? = null,
    ) = osakoeRepository.updateArvosana(
        id = osakoeId,
        arvosana = arvosana,
        arviointipaiva = arviointipaiva,
    )

    @WithSpan("VktSuoritusService.deleteOsakoe")
    fun deleteOsakoe(osakoeId: Int) = osakoeRepository.delete(osakoeId, retentionTimeForDeletedSeconds)

    @WithSpan("VktSuoritusService.markKoskiTransferProcessed")
    fun markKoskiTransferProcessed(
        id: Tutkintoryhma,
        koskiOpiskeluoikeusOid: String? = null,
    ) = customSuoritusRepository.markSuoritusTransferredToKoski(id, koskiOpiskeluoikeusOid)

    @WithSpan("VktSuoritusService.requestTransferToKoski")
    fun requestTransferToKoski(id: Tutkintoryhma) = customSuoritusRepository.requestTransferToKoski(id)

    private val listRowCounts =
        Cache(ttl = 5.minutes) { params: Triple<Koodisto.VktTaitotaso, Boolean?, String?> ->
            customSuoritusRepository.numberOfRowsForListView(
                taitotaso = params.first,
                arvioidut = params.second,
                searchQuery = params.third,
            )
        }

    @WithSpan("VktSuoritusService.cleanup")
    fun cleanup() {
        osakoeRepository.cleanup()
        customSuoritusRepository.cleanup()
    }

    companion object {
        const val PAGE_SIZE: Int = 50
    }
}
