package fi.oph.kitu.observability

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider
import io.opentelemetry.semconv.UrlAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetrySamplingRules {
    // Sample 1% of health check requests
    @Bean
    fun healthCheckSampling() =
        AutoConfigurationCustomizerProvider { customizer ->
            customizer.addSamplerCustomizer { fallback, _ ->
                RuleBasedRoutingSampler
                    .builder(SpanKind.SERVER, fallback)
                    .drop(UrlAttributes.URL_PATH, "/actuator/health")
                    .build()
            }
        }
}
