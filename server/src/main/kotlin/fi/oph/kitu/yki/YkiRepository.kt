package fi.oph.kitu.yki

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface YkiRepository : CrudRepository<YkiSuoritusEntity, Int>
