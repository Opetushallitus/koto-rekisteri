package fi.oph.kitu.oppija

import fi.oph.kitu.AUDIT_LOGGER_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppijaService(
    private val oppijaRepository: OppijaRepository,
) {
    private val auditLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)

    fun getAll(): Iterable<Oppija> = oppijaRepository.getAll()

    fun insert(name: String): Oppija? {
        val result = oppijaRepository.insert(name)

        auditLogger.atInfo().addKeyValue("example", "value").log("Inserted oppija")

        return result
    }
}
