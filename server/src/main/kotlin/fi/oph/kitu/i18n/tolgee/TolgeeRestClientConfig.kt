package fi.oph.kitu.i18n.tolgee

import fi.oph.kitu.config.ConditionalOnNonEmptyProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnNonEmptyProperty("kitu.tolgee.apiKey")
class TolgeeRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
    @param:Value($$"${kitu.tolgee.baseUrl}")
    private val baseUrl: String,
    @param:Value($$"${kitu.tolgee.apiKey}")
    private val apiKey: String,
) {
    @Bean("tolgeeRestClient")
    fun restClient(): RestClient =
        restClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", apiKey)
            .build()
}
