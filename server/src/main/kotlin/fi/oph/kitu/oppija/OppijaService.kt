package fi.oph.kitu.oppija

import org.springframework.stereotype.Service

@Service
class OppijaService(
    private val oppijaRepository: OppijaRepository,
) {
    fun getAll(): Iterable<Oppija> = oppijaRepository.getAll()

    fun insert(oppija: Oppija): Oppija? =
        oppijaRepository.insert(
            oppija.oid,
            oppija.firstName,
            oppija.lastName,
            oppija.hetu,
            oppija.nationality,
            oppija.gender,
            oppija.address,
            oppija.postalCode,
            oppija.city,
            oppija.email,
        )
}
