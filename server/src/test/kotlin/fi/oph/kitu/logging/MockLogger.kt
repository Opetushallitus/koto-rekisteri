package fi.oph.kitu.logging

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.spi.LoggingEventBuilder

class MockLogger(
    private val loggingEventBuilder: LoggingEventBuilder = MockEvent(),
) : Logger {
    override fun makeLoggingEventBuilder(level: Level?) = loggingEventBuilder

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun isTraceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTraceEnabled(p0: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: String?,
        p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: String?,
        p1: Any?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: String?,
        vararg p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: String?,
        p1: Throwable?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: Marker?,
        p1: String?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: Marker?,
        p1: String?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: Marker?,
        p1: String?,
        p2: Any?,
        p3: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: Marker?,
        p1: String?,
        vararg p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        p0: Marker?,
        p1: String?,
        p2: Throwable?,
    ) {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(p0: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: String?,
        p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: String?,
        p1: Any?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: String?,
        vararg p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: String?,
        p1: Throwable?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: Marker?,
        p1: String?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: Marker?,
        p1: String?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: Marker?,
        p1: String?,
        p2: Any?,
        p3: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: Marker?,
        p1: String?,
        vararg p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        p0: Marker?,
        p1: String?,
        p2: Throwable?,
    ) {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled() = true

    override fun isInfoEnabled(p0: Marker?) = true

    override fun info(p0: String?) {}

    override fun info(
        p0: String?,
        p1: Any?,
    ) {}

    override fun info(
        p0: String?,
        p1: Any?,
        p2: Any?,
    ) {}

    override fun info(
        p0: String?,
        vararg p1: Any?,
    ) {}

    override fun info(
        p0: String?,
        p1: Throwable?,
    ) {}

    override fun info(
        p0: Marker?,
        p1: String?,
    ) {}

    override fun info(
        p0: Marker?,
        p1: String?,
        p2: Any?,
    ) {}

    override fun info(
        p0: Marker?,
        p1: String?,
        p2: Any?,
        p3: Any?,
    ) {}

    override fun info(
        p0: Marker?,
        p1: String?,
        vararg p2: Any?,
    ) {}

    override fun info(
        p0: Marker?,
        p1: String?,
        p2: Throwable?,
    ) {}

    override fun isWarnEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isWarnEnabled(p0: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun warn(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: String?,
        p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: String?,
        vararg p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: String?,
        p1: Any?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: String?,
        p1: Throwable?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: Marker?,
        p1: String?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: Marker?,
        p1: String?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: Marker?,
        p1: String?,
        p2: Any?,
        p3: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: Marker?,
        p1: String?,
        vararg p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        p0: Marker?,
        p1: String?,
        p2: Throwable?,
    ) {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(p0: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: String?,
        p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: String?,
        p1: Any?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: String?,
        vararg p1: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: String?,
        p1: Throwable?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: Marker?,
        p1: String?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: Marker?,
        p1: String?,
        p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: Marker?,
        p1: String?,
        p2: Any?,
        p3: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: Marker?,
        p1: String?,
        vararg p2: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        p0: Marker?,
        p1: String?,
        p2: Throwable?,
    ) {
        TODO("Not yet implemented")
    }
}
