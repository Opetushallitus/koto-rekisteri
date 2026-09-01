package fi.oph.kitu.yki

import fi.oph.kitu.restclient.withBasicAuth
import fi.oph.kitu.restclient.withLenientStringConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class SolkiRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value($$"${kitu.yki.baseUrl}")
    private lateinit var baseUrl: String

    @Value($$"${kitu.yki.username}")
    private lateinit var user: String

    @Value($$"${kitu.yki.password}")
    private lateinit var password: String

    @Bean("solkiRestClient")
    fun restClient(): RestClient =
        restClientBuilder
            .baseUrl(baseUrl)
            .withBasicAuth(user, password)
            // retrieveEntitySafely(String::class.java) kaatuisi Jacksonin sisalla ilman tata:
            // Spring 7:n oletus-StringHttpMessageConverter mainostaa vain text/*.
            .withLenientStringConverter()
            .build()
}
