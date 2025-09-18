package fi.oph.kitu.observability

import fi.oph.kitu.logging.add
import io.opentelemetry.contrib.aws.resource.EcsResourceProvider
import io.opentelemetry.instrumentation.resources.ContainerResourceProvider
import io.opentelemetry.instrumentation.resources.HostIdResourceProvider
import io.opentelemetry.instrumentation.resources.HostResourceProvider
import io.opentelemetry.instrumentation.resources.ManifestResourceProvider
import io.opentelemetry.instrumentation.resources.OsResourceProvider
import io.opentelemetry.instrumentation.resources.ProcessResourceProvider
import io.opentelemetry.instrumentation.resources.ProcessRuntimeResourceProvider
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.resources.DistroVersionResourceProvider
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.resources.SpringResourceProvider
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider
import io.opentelemetry.sdk.resources.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Optional

@Configuration
class OpenTelemetryResourceConfiguration {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    // Specify resource providers explicitly instead of depending on OpenTelemetry's or Spring Boot actuator's autoconfiguration. The latter only detects its own resource providers and I can't get the former to do anything (possibly because Spring overrides it).
    @Bean
    fun resourceProviders(buildProperties: Optional<BuildProperties>): Set<ResourceProvider> =
        setOf(
            EcsResourceProvider(),
            SpringResourceProvider(buildProperties),
            DistroVersionResourceProvider(),
            ProcessRuntimeResourceProvider(),
            HostResourceProvider(),
            HostIdResourceProvider(),
            OsResourceProvider(),
            ProcessResourceProvider(),
            ContainerResourceProvider(),
            ManifestResourceProvider(),
        )
}

@Configuration
class OpenTelemetryListener(
    resourceProviders: Set<ResourceProvider>,
    resource: Resource,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger
            .atInfo()
            .add(
                "providers" to resourceProviders,
                "resource" to resource.attributes,
            ).log("Initialized OpenTelemetry resource")
    }
}
