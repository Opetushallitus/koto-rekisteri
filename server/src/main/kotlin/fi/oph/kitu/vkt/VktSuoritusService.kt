package fi.oph.kitu.vkt

import fi.oph.kitu.SortDirection
import fi.oph.kitu.findAllSorted
import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.vkt.html.VktIlmoittautuneet
import org.springframework.stereotype.Service

@Service
class VktSuoritusService(
    private val repository: VKTSuoritusRepository,
) {
    fun getIlmoittautuneet(
        sortColumn: VktIlmoittautuneet.Column,
        sortDirection: SortDirection,
    ): List<Henkilosuoritus<VktSuoritus>> =
        if (sortColumn == VktIlmoittautuneet.Column.Tutkintopaiva) {
            repository
                .findAllSorted(
                    VktIlmoittautuneet.Column.Sukunimi.dbColumn!!,
                    SortDirection.ASC,
                ).map { Henkilosuoritus.from(it) }
                .sortedBy { it.suoritus.tutkintopaiva }
        } else if (sortColumn.dbColumn != null) {
            repository
                .findAllSorted(
                    sortColumn.dbColumn,
                    sortDirection,
                ).map { Henkilosuoritus.from(it) }
        } else {
            repository.findAll().map { Henkilosuoritus.from(it) }
        }
}
