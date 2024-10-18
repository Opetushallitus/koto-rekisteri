package fi.oph.kitu

import org.slf4j.spi.LoggingEventBuilder
import org.springframework.http.ResponseEntity

inline fun <reified T> LoggingEventBuilder.addResponse(response: ResponseEntity<T>): LoggingEventBuilder {
    this.addKeyValue("response.headers", response.headers)
    this.addKeyValue("response.body", response.body)

    return this
}
