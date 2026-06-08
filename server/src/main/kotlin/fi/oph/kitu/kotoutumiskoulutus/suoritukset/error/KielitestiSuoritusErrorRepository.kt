package fi.oph.kitu.kotoutumiskoulutus.suoritukset.error

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface KielitestiSuoritusErrorRepository :
    CrudRepository<KielitestiSuoritusError, Int>,
    PagingAndSortingRepository<KielitestiSuoritusError, Int> {
    @Modifying
    @Query("DELETE FROM koto_suoritus_error WHERE completed = :completed")
    fun deleteAllByCompleted(
        @Param("completed") completed: Boolean,
    )
}
