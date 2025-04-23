package fi.oph.kitu.logging

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope

inline fun <T> Span.use(block: (Span) -> T): T {
    val scope: Scope = this.makeCurrent()
    return try {
        block(this)
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
    values: Array<String>,
): Span {
    for ((index, value) in values.withIndex()) {
        this.setAttribute("$key[$index]", value)
    }

    return this
}
