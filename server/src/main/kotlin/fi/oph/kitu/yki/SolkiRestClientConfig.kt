package fi.oph.kitu.yki

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.util.Base64

@Configuration
class SolkiRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.yki.baseUrl}")
    private lateinit var baseUrl: String

    @Value("\${kitu.yki.username}")
    private lateinit var user: String

    @Value("\${kitu.yki.password}")
    private lateinit var password: String

    @Bean("solkiRestClient")
    fun restClient(): RestClient {
        val basicAuthToken = Base64.getEncoder().encodeToString("$user:$password".toByteArray())
        return restClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Basic $basicAuthToken")
            .build()
    }
}
