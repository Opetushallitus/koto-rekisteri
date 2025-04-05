package fi.oph.kitu.logging

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryConfig {
    @Bean
    fun tracer(openTelemetry: OpenTelemetry): Tracer = openTelemetry.getTracer("Kitu")
}
