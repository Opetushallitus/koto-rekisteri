package fi.oph.kitu.vkt

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface VKTSuoritusRepository :
    CrudRepository<VKTSuoritusEntity, Int>,
    PagingAndSortingRepository<VKTSuoritusEntity, Int>
