package fi.oph.kitu.i18n

import fi.oph.kitu.config.ConditionalOnNonEmptyProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnNonEmptyProperty("kitu.lokalisointi.slug")
class LokalisointiRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value($$"${kitu.opintopolkuHostname}")
    private lateinit var opintopolkuHostname: String

    @Bean("lokalisointiRestClient")
    fun restClient(): RestClient =
        restClientBuilder
            .baseUrl("https://$opintopolkuHostname")
            .build()
}
