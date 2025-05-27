package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.sortedWithDirectionBy
import fi.oph.kitu.vkt.html.VktIlmoittautuneet
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Optional

@Service
class VktSuoritusService(
    private val suoritusRepository: VktSuoritusRepository,
    private val osakoeRepository: VktOsakoeRepository,
) {
    fun getIlmoittautuneet(
        sortColumn: VktIlmoittautuneet.Column,
        sortDirection: SortDirection,
    ): List<Henkilosuoritus<VktSuoritus>> {
        val latestIds = suoritusRepository.findIdsOfLatestVersions()

        return if (sortColumn == VktIlmoittautuneet.Column.Tutkintopaiva) {
            suoritusRepository
                .findAllByIdIn(latestIds)
                .map { Henkilosuoritus.from(it) }
                .sortedWithDirectionBy(sortDirection) { it.suoritus.tutkintopaiva }
        } else {
            suoritusRepository
                .findAllSortedByIdIn(latestIds, sortDirection.toSort(sortColumn.dbColumn!!))
                .map { Henkilosuoritus.from(it) }
        }
    }

    fun getSuoritus(id: Int): Optional<Henkilosuoritus<VktSuoritus>> =
        suoritusRepository
            .findById(id)
            .map { Henkilosuoritus.from(it) }

    fun setOsakoeArvosana(
        id: Int,
        arvosana: Koodisto.VktArvosana?,
        arviointipaiva: LocalDate? = null,
    ) = osakoeRepository.updateArvosana(id, arvosana, arviointipaiva)
}
