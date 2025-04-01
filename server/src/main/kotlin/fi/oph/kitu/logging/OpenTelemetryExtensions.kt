package fi.oph.kitu.logging

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope

inline fun <T> Span.use(block: (Span) -> T): T {
    val scope: Scope = this.makeCurrent()
    return try {
        block(this)
    } finally {
        scope.close()
        this.end()
    }
}
