package fi.oph.kitu.vkt

import fi.oph.kitu.Cache
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Optional
import kotlin.time.Duration.Companion.minutes

@Service
class VktSuoritusService(
    private val suoritusRepository: VktSuoritusRepository,
    private val customSuoritusRepository: CustomVktSuoritusRepository,
    private val osakoeRepository: VktOsakoeRepository,
) {
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

    fun getIlmoittautuneetForListView(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
        sortColumn: CustomVktSuoritusRepository.Column,
        sortDirection: SortDirection,
        pageNumber: Int,
        searchQuery: String?,
    ): List<Henkilosuoritus<VktSuoritus>> =
        customSuoritusRepository.findForListView(
            taitotaso = taitotaso,
            arvioidut = arvioidut,
            column = sortColumn,
            direction = sortDirection,
            limit = PAGE_SIZE,
            offset = (pageNumber - 1) * PAGE_SIZE,
            searchQuery = searchQuery,
        )

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

    fun getSuoritus(id: Int): Optional<Henkilosuoritus<VktSuoritus>> =
        suoritusRepository
            .findById(id)
            .map { Henkilosuoritus.from(it) }

    fun setOsakoeArvosana(
        id: Int,
        arvosana: Koodisto.VktArvosana?,
        arviointipaiva: LocalDate? = null,
    ) = osakoeRepository.updateArvosana(id, arvosana, arviointipaiva)

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
