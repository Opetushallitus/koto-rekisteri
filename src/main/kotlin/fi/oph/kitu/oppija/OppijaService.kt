package fi.oph.kitu.oppija

import org.springframework.stereotype.Service

@Service
class OppijaService(private val oppijaRepository: OppijaRepository) {
    fun getAll(): Iterable<Oppija> {
        return oppijaRepository.getAll()
    }

    fun insert(name: String): Oppija? {
        return oppijaRepository.insert(name)
    }
}
