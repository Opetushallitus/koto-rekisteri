package fi.oph.kitu.oauth2client

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient

@Profile("!test & !e2e")
@Configuration
class OAuth2RestClientConfiguration {
    @Value("\${kitu.palvelukayttaja.callerid}")
    private lateinit var callerId: String

    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        clientService: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder
                .builder()
                .refreshToken()
                .clientCredentials()
                .build()

        val authorizedClientManager =
            AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, clientService)

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)

        return authorizedClientManager
    }

    @Bean
    fun oauth2RestClient(
        builder: RestClient.Builder,
        authorizedClientManager: OAuth2AuthorizedClientManager,
    ): RestClient {
        val requestInterceptor =
            OAuth2ClientHttpRequestInterceptor(authorizedClientManager)

        requestInterceptor.setClientRegistrationIdResolver { "kielitutkintorekisteri-client" }

        return builder
            .requestInterceptor(requestInterceptor)
            .defaultHeaders { headers ->
                headers["Caller-Id"] = callerId
            }.build()
    }
}
