package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.vkt.html.VktIlmoittautuneet
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Optional

@Service
class VktSuoritusService(
    private val suoritusRepository: VKTSuoritusRepository,
    private val osakoeRepository: VKTOsakoeRepository,
) {
    fun getIlmoittautuneet(
        sortColumn: VktIlmoittautuneet.Column,
        sortDirection: SortDirection,
    ): List<Henkilosuoritus<VktSuoritus>> =
        if (sortColumn == VktIlmoittautuneet.Column.Tutkintopaiva) {
            suoritusRepository
                .findAllSorted(
                    VktIlmoittautuneet.Column.Sukunimi.dbColumn!!,
                    SortDirection.ASC,
                ).map { Henkilosuoritus.from(it) }
                .sortedBy { it.suoritus.tutkintopaiva }
        } else if (sortColumn.dbColumn != null) {
            suoritusRepository
                .findAllSorted(
                    sortColumn.dbColumn,
                    sortDirection,
                ).map { Henkilosuoritus.from(it) }
        } else {
            suoritusRepository.findAll().map { Henkilosuoritus.from(it) }
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
