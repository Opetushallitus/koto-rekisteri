package fi.oph.kitu.observability

import arrow.core.Either
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Scope

inline fun <T> Span.use(block: (Span) -> T): T {
    val scope: Scope = this.makeCurrent()
    return try {
        block(this).also {
            this.setAttributesForEither(it)
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

fun <T> Span.setAttributesForEither(result: T) {
    this.setAttribute("isEither", result is Either<*, *>)
    if (result is Either<*, *>) {
        this.setAttribute("Either.isRight", result.isRight())
        if (result is Either.Left<*>) {
            this.setStatus(StatusCode.ERROR)
            (result.value as? Throwable)?.let { this.recordException(it) }
        }
    }
}
