package fi.oph.kitu.oppija

import org.springframework.stereotype.Service

@Service
class OppijaService(
    private val oppijaRepository: OppijaRepository,
) {
    fun getAll(): Iterable<Oppija> = oppijaRepository.getAll()

    fun insert(name: String): Oppija? = oppijaRepository.insert(name)
}
