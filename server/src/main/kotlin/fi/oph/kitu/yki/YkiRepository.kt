package fi.oph.kitu.yki

import fi.oph.kitu.yki.entities.YkiSuoritusEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface YkiRepository : CrudRepository<YkiSuoritusEntity, Int>
