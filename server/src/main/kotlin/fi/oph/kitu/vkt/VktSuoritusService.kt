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
    ): List<Henkilosuoritus<VktSuoritus>> {
        val rows =
            if (sortColumn.dbColumn != null) {
                repository.findAllSorted(
                    sortColumn.dbColumn,
                    sortDirection,
                )
            } else {
                repository.findAll()
            }
        return rows.map { Henkilosuoritus.from(it) }
    }
}
