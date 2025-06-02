package fi.oph.kitu

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestClient

@Configuration
class DefaultRestClientBuilderConfig {
    @Bean
    @Primary
    fun defaultRestClientBuilder(): RestClient.Builder = RestClient.builder()
}
