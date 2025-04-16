package fi.oph.kitu.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import org.springframework.boot.logging.structured.StructuredLogFormatter
import java.time.Instant

class PrettyStructuredLogFormatter : StructuredLogFormatter<ILoggingEvent> {
    override fun format(event: ILoggingEvent): String {
        val messageCandidates = listOf(event.formattedMessage, event.throwableProxy.message)
        val message = messageCandidates.find { !it.isNullOrBlank() } ?: "<no message>"
        val timestamp = "[${Instant.ofEpochMilli(event.timeStamp)}]"
        val importantAttributes =
            listOf(
                "thread=${event.threadName}",
                "level=${event.level}",
                "logger=${event.loggerName}",
            )
        val otherAttributes =
            event.mdcPropertyMap.entries.map { "${it.key}=${it.value}" } +
                event.keyValuePairs.map { "${it.key}=${it.value}" }
        val elements = listOf(timestamp, message) + importantAttributes + otherAttributes
        return elements.joinToString(" ") + "\n"
    }
}
