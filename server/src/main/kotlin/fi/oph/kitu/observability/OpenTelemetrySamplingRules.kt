package fi.oph.kitu.observability

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.semconv.UrlAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetrySamplingRules {
    // Sample 1% of health check requests
    @Bean
    fun healthCheckSampling() =
        object : AutoConfigurationCustomizerProvider {
            override fun customize(customizer: AutoConfigurationCustomizer) {
                customizer.addSamplerCustomizer { fallback, config ->
                    RuleBasedRoutingSampler
                        .builder(SpanKind.SERVER, fallback)
                        .customize(UrlAttributes.URL_PATH, "/actuator/health", Sampler.traceIdRatioBased(0.01))
                        .build()
                }
            }
        }
}
