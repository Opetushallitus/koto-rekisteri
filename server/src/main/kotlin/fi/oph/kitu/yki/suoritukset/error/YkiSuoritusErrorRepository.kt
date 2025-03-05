package fi.oph.kitu.yki.suoritukset.error

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface YkiSuoritusErrorRepository : CrudRepository<YkiSuoritusErrorEntity, Long>
