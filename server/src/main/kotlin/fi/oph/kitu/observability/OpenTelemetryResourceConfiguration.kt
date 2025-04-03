package fi.oph.kitu.observability

import fi.oph.kitu.logging.add
import io.opentelemetry.contrib.aws.resource.EcsResourceProvider
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider
import io.opentelemetry.sdk.resources.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryResourceConfiguration {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun ecsResourceProvider(): ConditionalResourceProvider =
        object : ConditionalResourceProvider {
            private val ecsResourceProvider =
                EcsResourceProvider().also { provider ->
                    logger.atInfo().log("EcsResourceProvider loaded")
                }

            override fun createResource(config: ConfigProperties): Resource? =
                ecsResourceProvider.createResource(config).also { resource ->
                    logger.atInfo().add("attributes" to resource.attributes).log("EcsResourceProvider created")
                }

            override fun shouldApply(
                config: ConfigProperties,
                existing: Resource,
            ): Boolean =
                ecsResourceProvider.shouldApply(config, existing).let { result ->
                    logger
                        .atInfo()
                        .add(
                            "shouldApply" to result,
                            "config" to config,
                            "existing" to existing.attributes,
                        ).log("EcsResourceProvider shouldApply")
                    return true // override shouldApply
                }
        }
}
