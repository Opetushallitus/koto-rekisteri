package fi.oph.kitu.logging

import fi.oph.kitu.setAttributesForTypedResult
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope

inline fun <T> Span.use(block: (Span) -> T): T {
    val scope: Scope = this.makeCurrent()
    return try {
        block(this).also {
            this.setAttributesForTypedResult(it)
        }
    } catch (e: Throwable) {
        this.recordException(e)
        throw e
    } finally {
        scope.close()
        this.end()
    }
}

fun Span.setAttribute(
    key: String,
    value: Int,
): Span = this.setAttribute(key, value.toLong())
