package fi.oph.kitu.kotoutumiskoulutus

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface KielitestiSuoritusErrorsRepository :
    CrudRepository<KielitestiSuoritusError, Int>,
    PagingAndSortingRepository<KielitestiSuoritusError, Int>
