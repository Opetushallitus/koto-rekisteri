package fi.oph.kitu.oauth2client

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient

@Profile("!test & !e2e")
@Configuration
class OAuth2RestClientConfiguration {
    @Value("\${kitu.palvelukayttaja.callerid}")
    private lateinit var callerId: String

    @Bean
    fun oauth2RestClient(
        builder: RestClient.Builder,
        authorizedClientManager: OAuth2AuthorizedClientManager,
    ): RestClient {
        val requestInterceptor =
            OAuth2ClientHttpRequestInterceptor(authorizedClientManager)

        return builder
            .requestInterceptor(requestInterceptor)
            .defaultHeaders { headers ->
                headers["Caller-Id"] = callerId
            }.build()
    }
}
