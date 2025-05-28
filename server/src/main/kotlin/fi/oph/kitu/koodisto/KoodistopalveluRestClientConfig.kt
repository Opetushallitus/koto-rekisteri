package fi.oph.kitu.koodisto

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class KoodistopalveluRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.koodistopalvelu.baseUrl}")
    private lateinit var koodistopalveluBaseUrl: String

    @Bean("koodistopalveluRestClient")
    fun restClient(): RestClient =
        restClientBuilder
            .baseUrl(koodistopalveluBaseUrl)
            .build()
}
