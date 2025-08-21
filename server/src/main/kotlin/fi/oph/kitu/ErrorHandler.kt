package fi.oph.kitu

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    val message: String?,
)

@ControllerAdvice
class GlobalControllerExceptionHandler {
    private val logger: Logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler::class.java)

    @ExceptionHandler
    fun handleValidationException(e: Validation.ValidationException): ResponseEntity<RestErrorMessage> =
        ResponseEntity(
            RestErrorMessage(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad request: validation error",
                message = e.errors.joinToString(", "),
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
                message = "Call to external API failed",
            ),
            HttpStatus.SERVICE_UNAVAILABLE,
        )
    }
}
