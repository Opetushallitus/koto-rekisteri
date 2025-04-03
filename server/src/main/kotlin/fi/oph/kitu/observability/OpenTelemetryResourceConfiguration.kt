package fi.oph.kitu.observability

import fi.oph.kitu.logging.add
import io.opentelemetry.contrib.aws.resource.EcsResourceProvider
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider
import io.opentelemetry.sdk.resources.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryResourceConfiguration {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun ecsResourceProvider(): ResourceProvider =
        object : ResourceProvider {
            private val ecsResourceProvider =
                EcsResourceProvider().also {
                    logger.atInfo().log("EcsResourceProvider loaded")
                }

            override fun createResource(config: ConfigProperties): Resource? =
                ecsResourceProvider.createResource(config).also { resource ->
                    logger.atInfo().add("attributes" to resource.attributes).log("EcsResourceProvider created")
                }
        }
}
