package fi.oph.kitu

import fi.oph.kitu.html.ErrorPage
import io.opentelemetry.api.trace.Span
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.RestClientException
import java.time.Instant

data class RestErrorMessage(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val messages: List<String>?,
)

@ControllerAdvice
class GlobalControllerExceptionHandler(
    val environment: Environment,
) {
    private val logger: Logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler::class.java)

    @Value("\${trace-ui:}")
    private lateinit var traceUiUrl: String

    @ExceptionHandler
    fun handleValidationException(e: Validation.ValidationException): ResponseEntity<RestErrorMessage> =
        ResponseEntity(
            RestErrorMessage(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad request: validation error",
                messages = e.errors.map { it.toString() },
            ),
            HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler
    fun handleRestClientException(ex: RestClientException): ResponseEntity<RestErrorMessage> {
        logger.error(ex.stackTraceToString())
        return ResponseEntity(
            RestErrorMessage(
                status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                error = "Service Unavailable",
                messages = listOf("Call to external API failed"),
            ),
            HttpStatus.SERVICE_UNAVAILABLE,
        )
    }

    @ExceptionHandler
    fun handleServerException(error: Throwable): ResponseEntity<String> {
        val traceId = Span.current().spanContext?.traceId
        val isLocal = environment.activeProfiles.contains("local")
        val traceUrl =
            if (traceId !== null &&
                traceUiUrl.isNotEmpty()
            ) {
                traceUiUrl.replace("{traceId}", traceId)
            } else {
                null
            }
        return ResponseEntity(ErrorPage.render(error, traceId, traceUrl, isLocal), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
