package fi.oph.kitu

import fi.oph.kitu.kielitesti.MoodleException
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

    @ExceptionHandler
    fun handleMoodleException(ex: MoodleException): ResponseEntity<RestErrorMessage> {
        logger.error(ex.moodleErrorMessage.toString())
        return ResponseEntity(
            RestErrorMessage(
                status = HttpStatus.SERVICE_UNAVAILABLE.value(),
                error = "${ex.moodleErrorMessage.exception}: ${ex.moodleErrorMessage.errorcode}",
                message = "Call to external API failed",
            ),
            HttpStatus.SERVICE_UNAVAILABLE,
        )
    }
}
