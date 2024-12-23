package fi.oph.kitu.oppijanumero

import fi.oph.kitu.logging.add
import org.slf4j.spi.LoggingEventBuilder
import java.net.http.HttpResponse

class CasException(
    val response: HttpResponse<String>,
    message: String,
) : Throwable(message)

fun Result<String>.getOrLogAndThrowCasException(event: LoggingEventBuilder): String =
    this
        .onFailure { cause ->
            when (cause) {
                is CasException ->
                    event.add(
                        "cas-error.response.statusCode" to cause.response.statusCode(),
                        "cas-error.response.uri" to cause.response.uri(),
                        "cas-error.response.headers" to cause.response.headers(),
                        "cas-error.response.body" to cause.response.body(),
                        "cas-error.exception.message" to cause.message,
                    )
            }
        }.getOrThrow()
