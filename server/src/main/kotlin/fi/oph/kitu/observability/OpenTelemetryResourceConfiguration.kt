package fi.oph.kitu.observability

import io.opentelemetry.contrib.aws.resource.EcsResourceProvider
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryResourceConfiguration {
    @Bean
    fun ecsResourceProvider(): ResourceProvider = EcsResourceProvider()
}
