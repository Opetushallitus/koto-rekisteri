package fi.oph.kitu.vkt

import fi.oph.kitu.Cache
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.logging.AuditLogger
import fi.oph.kitu.logging.KituAuditLogMessageField
import fi.oph.kitu.logging.KituAuditLogOperation
import fi.oph.kitu.vkt.CustomVktSuoritusRepository.Tutkintoryhma
import fi.oph.kitu.vkt.html.VktTableItem
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Optional
import kotlin.collections.listOf
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.minutes

@Service
class VktSuoritusService(
    private val suoritusRepository: VktSuoritusRepository,
    private val customSuoritusRepository: CustomVktSuoritusRepository,
    private val osakoeRepository: VktOsakoeRepository,
    private val auditLogger: AuditLogger,
) {
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
    fun getSuoritus(id: Int): Optional<Henkilosuoritus<VktSuoritus>> =
        suoritusRepository
            .findById(id)
            .map { Henkilosuoritus.from(it) }

    @WithSpan("VktSuoritusService.getOppijanSuoritukset")
    fun getOppijanSuoritukset(id: Tutkintoryhma): Henkilosuoritus<VktSuoritus>? {
        val ids = customSuoritusRepository.getOppijanSuoritusIds(id)
        val suoritukset =
            ids
                .mapNotNull { suoritusRepository.findById(it).getOrNull() }
                .map { Henkilosuoritus.from(it) }
                .also {
                    it.firstOrNull()?.henkilo?.let { henkilo ->
                        auditLogger.log(
                            operation = KituAuditLogOperation.VktSuoritusViewed,
                            oppijaHenkiloOid = henkilo.oid.oid,
                            target =
                                listOf(
                                    Pair(KituAuditLogMessageField.OPPIJA_OPPIJANUMERO, henkilo.oid.oid),
                                ),
                        )
                    }
                }
        return if (suoritukset.isEmpty()) null else VktSuoritus.merge(suoritukset)
    }

    @WithSpan("VktSuoritusService.setOsakoeArvosana")
    fun setOsakoeArvosana(
        id: Int,
        arvosana: Koodisto.VktArvosana?,
        arviointipaiva: LocalDate? = null,
    ) = osakoeRepository.updateArvosana(id, arvosana, arviointipaiva)

    @WithSpan("VktSuoritusServer.markKoskiTransferProcessed")
    fun markKoskiTransferProcessed(
        id: Tutkintoryhma,
        koskiOpiskeluoikeusOid: String? = null,
    ) = customSuoritusRepository.markSuoritusTransferredToKoski(id, koskiOpiskeluoikeusOid)

    @WithSpan("VktSuoritusServer.requestTransferToKoski")
    fun requestTransferToKoski(id: Tutkintoryhma) = customSuoritusRepository.requestTransferToKoski(id)

    private val listRowCounts =
        Cache(ttl = 5.minutes) { params: Triple<Koodisto.VktTaitotaso, Boolean?, String?> ->
            customSuoritusRepository.numberOfRowsForListView(
                taitotaso = params.first,
                arvioidut = params.second,
                searchQuery = params.third,
            )
        }

    companion object {
        const val PAGE_SIZE: Int = 50
    }
}
