package fi.oph.kitu.html

import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.pre

object ErrorPage {
    fun render(
        error: Throwable,
        traceId: String?,
        traceUrl: String?,
        isLocal: Boolean,
    ): String =
        Page.renderHtml(emptyList()) {
            h1 {
                if (isLocal) {
                    +error.toString()
                } else {
                    +"Internal server error"
                }
            }

            traceId?.let {
                p {
                    +"Trace ID: "
                    if (traceUrl != null) {
                        a(href = traceUrl, target = "_blank") { +it }
                    } else {
                        +it
                    }
                }
            }

            if (isLocal) {
                pre { +error.stackTraceToString() }
            }
        }
}
