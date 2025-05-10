package fi.oph.kitu.logging

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import java.util.concurrent.TimeUnit

/*
 * Don't use this on integration tests, use the actual tracer with inmemory exporter
 */
class MockTracer : Tracer {
    override fun spanBuilder(spanName: String): SpanBuilder? = MockSpanbuilder()
}

class MockSpanContext : SpanContext {
    override fun getTraceId(): String? = null

    override fun getSpanId(): String? = null

    override fun getTraceFlags(): TraceFlags? = null

    override fun getTraceState(): TraceState? = null

    override fun isRemote(): Boolean = false
}

class MockSpan : Span {
    override fun <T : Any?> setAttribute(
        key: AttributeKey<T?>,
        value: T & Any,
    ): Span? = MockSpan()

    override fun addEvent(
        name: String,
        attributes: Attributes,
    ): Span? {
        TODO("Not yet implemented")
    }

    override fun addEvent(
        name: String,
        attributes: Attributes,
        timestamp: Long,
        unit: TimeUnit,
    ): Span? {
        TODO("Not yet implemented")
    }

    override fun setStatus(
        statusCode: StatusCode,
        description: String,
    ): Span? {
        TODO("Not yet implemented")
    }

    override fun recordException(
        exception: Throwable,
        additionalAttributes: Attributes,
    ): Span? = MockSpan()

    override fun updateName(name: String): Span? {
        TODO("Not yet implemented")
    }

    override fun end() {}

    override fun end(
        timestamp: Long,
        unit: TimeUnit,
    ) {
        TODO("Not yet implemented")
    }

    override fun getSpanContext(): SpanContext? = MockSpanContext()

    override fun isRecording(): Boolean {
        TODO("Not yet implemented")
    }
}

class MockSpanbuilder : SpanBuilder {
    override fun setParent(context: Context): SpanBuilder? {
        TODO("setParent: Not yet implemented")
    }

    override fun setNoParent(): SpanBuilder? {
        TODO("setNoParent: Not yet implemented")
    }

    override fun addLink(spanContext: SpanContext): SpanBuilder? {
        TODO("addLink: Not yet implemented")
    }

    override fun addLink(
        spanContext: SpanContext,
        attributes: Attributes,
    ): SpanBuilder? {
        TODO("addLink2: Not yet implemented")
    }

    override fun setAttribute(
        key: String,
        value: String,
    ): SpanBuilder? {
        TODO("setAttribute: Not yet implemented")
    }

    override fun setAttribute(
        key: String,
        value: Long,
    ): SpanBuilder? {
        TODO("setAttribute2: Not yet implemented")
    }

    override fun setAttribute(
        key: String,
        value: Double,
    ): SpanBuilder? {
        TODO("setAttribute3: Not yet implemented")
    }

    override fun setAttribute(
        key: String,
        value: Boolean,
    ): SpanBuilder? {
        TODO("setAttribute4: Not yet implemented")
    }

    override fun <T : Any?> setAttribute(
        key: AttributeKey<T?>,
        value: T & Any,
    ): SpanBuilder? {
        TODO("setAttribute5: Not yet implemented")
    }

    override fun setSpanKind(spanKind: SpanKind): SpanBuilder? {
        TODO("setSpanKind: Not yet implemented")
    }

    override fun setStartTimestamp(
        startTimestamp: Long,
        unit: TimeUnit,
    ): SpanBuilder? {
        TODO("setStartTimestamp: Not yet implemented")
    }

    override fun startSpan(): Span? = MockSpan()
}
