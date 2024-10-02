package fi.oph.kitu.yki

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class YkiService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset() {
        logger.info("Running import for \"YKI suoritukset\"")
        // TODO: the implementation goes here
    }
}
