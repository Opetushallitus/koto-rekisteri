package fi.oph.kitu.html

import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.pre
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object ErrorPage {
    fun error(
        error: Throwable,
        traceId: String?,
        traceUrl: String?,
        isLocal: Boolean,
    ): ResponseEntity<String> =
        ResponseEntity(
            Page.renderHtml {
                h1 {
                    if (isLocal) {
                        +error.toString()
                    } else {
                        +"Internal server error"
                    }
                }

                traceInfo(traceId, traceUrl)

                if (isLocal) {
                    pre { +error.stackTraceToString() }
                }
            },
            HttpStatus.INTERNAL_SERVER_ERROR,
        )

    fun notFound(
        traceId: String?,
        traceUrl: String?,
    ): ResponseEntity<String> =
        ResponseEntity(
            Page.renderHtml {
                h1 { +"Sivua ei löydy" }
                traceInfo(traceId, traceUrl)
            },
            HttpStatus.NOT_FOUND,
        )

    fun badRequest(
        traceId: String?,
        traceUrl: String?,
    ): ResponseEntity<String> =
        ResponseEntity(
            Page.renderHtml {
                h1 { +"Virheellinen pyyntö" }
                p { +"Tarkista että esimerkiksi sivun osoitteen kaikki parametrit on kirjoitettu oikein." }
                traceInfo(traceId, traceUrl)
            },
            HttpStatus.BAD_REQUEST,
        )

    fun FlowContent.traceInfo(
        traceId: String?,
        traceUrl: String?,
    ) {
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
    }
}
