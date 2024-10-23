package fi.oph.kitu.yki

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class SolkiRestClientConfig {
    @Value("\${kitu.yki.baseUrl}")
    private lateinit var baseUrl: String

    @Bean("solkiRestClient")
    fun restClient(): RestClient =
        RestClient
            .builder()
            .baseUrl(baseUrl)
            .build()
}
