package fi.oph.kitu.yki

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class MockClientConfig {
    @Bean("mockRestClient")
    fun restClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("http://localhost:8080/dev/mock/")
            .build()
}
