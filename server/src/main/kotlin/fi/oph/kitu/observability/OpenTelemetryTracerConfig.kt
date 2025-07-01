package fi.oph.kitu.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryTracerConfig {
    @Bean
    fun tracer(openTelemetry: OpenTelemetry): Tracer = openTelemetry.getTracer("Kitu")
}
