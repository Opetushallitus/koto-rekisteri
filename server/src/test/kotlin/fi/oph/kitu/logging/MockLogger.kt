package fi.oph.kitu.logging

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.spi.LoggingEventBuilder

class MockLogger(
    val mockedEvent: LoggingEventBuilder = MockEvent(),
) : Logger {
    override fun makeLoggingEventBuilder(level: Level?): LoggingEventBuilder = mockedEvent

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

    override fun isInfoEnabled(): Boolean = true

    override fun isInfoEnabled(p0: Marker?) = true

    override fun info(p0: String?) {
    }

    override fun info(
        p0: String?,
        p1: Any?,
    ) {
        println("info1")
    }

    override fun info(
        p0: String?,
        p1: Any?,
        p2: Any?,
    ) {
        println("info2")
    }

    override fun info(
        p0: String?,
        vararg p1: Any?,
    ) {
        println("info3")
    }

    override fun info(
        p0: String?,
        p1: Throwable?,
    ) {
        println("info4")
    }

    override fun info(
        p0: Marker?,
        p1: String?,
    ) {
        println("info5")
    }

    override fun info(
        p0: Marker?,
        p1: String?,
        p2: Any?,
    ) {
        println("info6")
    }

    override fun info(
        p0: Marker?,
        p1: String?,
        p2: Any?,
        p3: Any?,
    ) {
        println("info7")
    }

    override fun info(
        p0: Marker?,
        p1: String?,
        vararg p2: Any?,
    ) {
        println("info8")
    }

    override fun info(
        p0: Marker?,
        p1: String?,
        p2: Throwable?,
    ) {
        println("info9")
    }

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
