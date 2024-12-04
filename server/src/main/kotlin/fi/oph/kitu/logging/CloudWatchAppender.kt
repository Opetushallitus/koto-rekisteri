package fi.oph.kitu.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.encoder.Encoder
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest

// Copied from https://github.com/Opetushallitus/ludos/blob/main/server/src/main/kotlin/fi/oph/ludos/aws/LudosLogbackCloudwatchAppender.kt
class CloudwatchAppender : AppenderBase<ILoggingEvent>() {
    // <logback-spring.xml-attributes>
    var encoder: Encoder<ILoggingEvent>? = null
    var logGroupName: String? = null
    // </logback-spring.xml-attributes>

    private lateinit var cloudWatchLogsClient: CloudWatchLogsClient

    private val logStreamName: String = getEcsTaskIdFromEnv()

    override fun start() {
        if (started) {
            return
        }

        checkNotNull(encoder) { "encoder was not set for appender" }
        checkNotNull(logGroupName) { "logGroupName was not set for appender" }
        if (logGroupName!!.isBlank()) {
            throw IllegalStateException("logGroupName was blank")
        }

        cloudWatchLogsClient = CloudWatchLogsClient.create()
        val createLogStreamRequest =
            CreateLogStreamRequest
                .builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .build()
        cloudWatchLogsClient.createLogStream(createLogStreamRequest)

        super.start()
    }

    override fun append(eventObject: ILoggingEvent) {
        val message = encoder?.encode(eventObject)?.toString(Charsets.UTF_8) ?: eventObject.formattedMessage
        val request =
            PutLogEventsRequest
                .builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .logEvents(
                    InputLogEvent
                        .builder()
                        .message(message)
                        .timestamp(eventObject.timeStamp)
                        .build(),
                ).build()

        try {
            cloudWatchLogsClient.putLogEvents(request)
        } catch (e: Throwable) {
            // Don't propagate exception.
            Span
                .current()
                .recordException(
                    e,
                ).setStatus(StatusCode.ERROR, "Error calling put-log-events(${eventObject.formattedMessage})")
        }
    }
}

fun getEcsTaskIdFromEnv(): String {
    // Esimerkki ECS_CONTAINER_METADATA_URI:n muodosta:
    // http://169.254.170.2/v3/2df36241782d45e5ad3816ea5cc10f61-2990360344
    val metadataUri = System.getenv("ECS_CONTAINER_METADATA_URI")
    val taskId =
        metadataUri
            ?.split("/")
            ?.last()
            ?.split("-")
            ?.first()
    return taskId ?: ("unknown_task_id-" + System.currentTimeMillis())
}
