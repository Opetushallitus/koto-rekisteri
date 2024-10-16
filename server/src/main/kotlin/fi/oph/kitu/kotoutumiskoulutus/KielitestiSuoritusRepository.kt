package fi.oph.kitu.kotoutumiskoulutus

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface KielitestiSuoritusRepository : CrudRepository<KielitestiSuoritus, Int>
