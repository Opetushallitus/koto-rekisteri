package fi.oph.kitu

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
    @ExceptionHandler
    fun handleRestClientException(ex: RestClientException): ResponseEntity<RestErrorMessage> =
        ResponseEntity(
            RestErrorMessage(
                status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                error = "Service Unavailable",
                message = "Call to external API failed",
            ),
            HttpStatus.SERVICE_UNAVAILABLE,
        )
}
