package fi.oph.kitu.koski

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.util.Base64

@Configuration
class KoskiRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.koski.baseUrl}")
    private lateinit var koskiBaseUrl: String

    @Value("\${kitu.palvelukayttaja.username}")
    private lateinit var user: String

    @Value("\${kitu.palvelukayttaja.password}")
    private lateinit var password: String

    @Bean("koskiRestClient")
    fun restClient(): RestClient {
        val basicAuthToken = Base64.getEncoder().encodeToString("$user:$password".toByteArray())
        return restClientBuilder
            .baseUrl(koskiBaseUrl)
            .defaultHeader("Authorization", "Basic $basicAuthToken")
            .build()
    }
}
