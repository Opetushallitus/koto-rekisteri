package fi.oph.kitu.vkt

import fi.oph.kitu.Cache
import fi.oph.kitu.SortDirection
import fi.oph.kitu.html.Pagination
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.html.VktIlmoittautuneet
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
    fun getIlmoittautuneetAndPagination(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
        sortColumn: VktIlmoittautuneet.Column,
        sortDirection: SortDirection,
        pageNumber: Int,
    ) = Pair(
        getIlmoittautuneetForListView(taitotaso, arvioidut, sortColumn, sortDirection, pageNumber),
        getPagination(sortColumn, sortDirection, pageNumber, taitotaso, arvioidut),
    )

    fun getIlmoittautuneetForListView(
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
        sortColumn: VktIlmoittautuneet.Column,
        sortDirection: SortDirection,
        pageNumber: Int,
    ): List<Henkilosuoritus<VktSuoritus>> =
        customSuoritusRepository.findForListView(
            taitotaso = taitotaso,
            arvioidut = arvioidut,
            column = sortColumn,
            direction = sortDirection,
            limit = PAGE_SIZE,
            offset = (pageNumber - 1) * PAGE_SIZE,
        )

    fun getPagination(
        sortColumn: VktIlmoittautuneet.Column,
        sortDirection: SortDirection,
        currentPageNumber: Int,
        taitotaso: Koodisto.VktTaitotaso,
        arvioidut: Boolean?,
    ): Pagination =
        Pagination.valueOf(
            currentPageNumber = currentPageNumber,
            numberOfRows = listRowCounts.get(Pair(taitotaso, arvioidut))!!,
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
        Cache(ttl = 5.minutes) { params: Pair<Koodisto.VktTaitotaso, Boolean?> ->
            customSuoritusRepository.numberOfRowsForListView(
                taitotaso = params.first,
                arvioidut = params.second,
            )
        }

    companion object {
        const val PAGE_SIZE: Int = 50
    }
}
