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
                        "cas.response.statusCode" to cause.response.statusCode(),
                        "cas.response.uri" to cause.response.uri(),
                        "cas.response.headers" to cause.response.headers(),
                        "cas.response.body" to cause.response.body(),
                        "cas.exception.message" to cause.message,
                    )
            }
        }.getOrThrow()
