package fi.oph.kitu.oppija

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OppijaService(
    private val oppijaRepository: OppijaRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.kielitesti.wstoken}")
    lateinit var moodleToken: String

    fun getAll(): Iterable<Oppija> {
        logger.info("moodle token: $moodleToken")
        return oppijaRepository.getAll()
    }

    fun insert(name: String): Oppija? = oppijaRepository.insert(name)
}
