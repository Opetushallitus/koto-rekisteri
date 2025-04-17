package fi.oph.kitu.kotoutumiskoulutus

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface KielitestiSuoritusErrorRepository :
    CrudRepository<KielitestiSuoritusError, Int>,
    PagingAndSortingRepository<KielitestiSuoritusError, Int>
