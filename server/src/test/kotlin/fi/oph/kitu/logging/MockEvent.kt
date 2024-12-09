package fi.oph.kitu.logging

import org.slf4j.Marker
import org.slf4j.spi.LoggingEventBuilder
import java.util.function.Supplier

class MockEvent : LoggingEventBuilder {
    val keyValues = mutableListOf<Pair<String?, Any?>>()
    val causes = mutableListOf<Throwable?>()
    val markers = mutableListOf<Marker?>()
    val arguments = mutableListOf<Any?>()
    val messages = mutableListOf<String?>()
    val logs = mutableListOf<String?>()
    val defaultLogValue: String = "NULL"

    override fun setCause(p0: Throwable?): LoggingEventBuilder {
        causes.add(p0)
        return this
    }

    override fun addMarker(p0: Marker?): LoggingEventBuilder {
        markers.add(p0)
        return this
    }

    override fun addArgument(p0: Any?): LoggingEventBuilder {
        arguments.add(p0)
        return this
    }

    override fun addArgument(p0: Supplier<*>?): LoggingEventBuilder {
        arguments.add(p0?.get())
        return this
    }

    override fun addKeyValue(
        p0: String?,
        p1: Any?,
    ): LoggingEventBuilder {
        keyValues.add(Pair(p0, p1))
        return this
    }

    override fun addKeyValue(
        p0: String?,
        p1: Supplier<Any>?,
    ): LoggingEventBuilder {
        keyValues.add(Pair(p0, p1?.get()))
        return this
    }

    override fun setMessage(p0: String?): LoggingEventBuilder {
        messages.add(p0)
        return this
    }

    override fun setMessage(p0: Supplier<String>?): LoggingEventBuilder {
        messages.add(p0?.get())
        return this
    }

    override fun log() {
        logs.add(defaultLogValue)
    }

    override fun log(p0: String?) {
        logs.add(p0)
    }

    override fun log(
        p0: String?,
        p1: Any?,
    ) {
        logs.add(p0?.format(p1) ?: defaultLogValue)
    }

    override fun log(
        p0: String?,
        p1: Any?,
        p2: Any?,
    ) {
        logs.add(p0?.format(p1, p2) ?: defaultLogValue)
    }

    override fun log(
        p0: String?,
        vararg p1: Any?,
    ) {
        logs.add(p0?.format(p1) ?: defaultLogValue)
    }

    override fun log(p0: Supplier<String>?) {
        logs.add(p0?.get() ?: defaultLogValue)
    }
}
