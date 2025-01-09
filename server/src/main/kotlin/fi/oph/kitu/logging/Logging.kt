package fi.oph.kitu.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val AUDIT_LOGGER_NAME = "auditLogger"

object Logging {
    fun auditLogger(): Logger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)
}
