package fi.oph.kitu

import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.core.configuration.SpringDocUIConfiguration
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.core.providers.ObjectMapperProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Optional

/**
 * Provides minimal required Swagger UI configuration beans.
 *
 * As per springdoc FAQ (2024-09-12), the proper way to set up the Swagger UI with provided spec.yaml is to
 * 1. Disable all default SpringDoc `springdoc-openapi` autoconfiguration beans
 * 2. Provide a custom configuration
 * 3. Point Swagger UI to the provided spec.yaml
 *
 * The 1. and 3. are achieved through properties `springdoc.api-docs.enabled` (1.) and `springdoc.swagger-ui.url` (3.)
 * set via `application.properties`. This class provides the custom configuration beans. As our needs are quite basic,
 * this mostly means just a minimal configuration (= class defaults).
 *
 * @see <a href="https://springdoc.org/#what-is-a-proper-way-to-set-up-swagger-ui-to-use-provided-spec-yml">SpringDoc FAQ</a>
 */
@Configuration
class SwaggerUIConfig {
    @Bean
    fun springDocConfiguration(): SpringDocConfiguration = SpringDocConfiguration()

    @Bean
    fun springDocConfigProperties(): SpringDocConfigProperties = SpringDocConfigProperties()

    @Bean
    fun objectMapperProvider(springDocConfigProperties: SpringDocConfigProperties): ObjectMapperProvider =
        ObjectMapperProvider(springDocConfigProperties)

    @Bean
    fun springDocUIConfiguration(
        optionalSwaggerUiConfigProperties: Optional<SwaggerUiConfigProperties>,
    ): SpringDocUIConfiguration = SpringDocUIConfiguration(optionalSwaggerUiConfigProperties)
}
