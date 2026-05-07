package fi.oph.kitu.dev

import fi.oph.kitu.restclient.withLenientStringConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestClient

@Profile("test", "e2e")
@Configuration
class MockOAuth2RestClientConfiguration {
    @Bean
    fun oauth2RestClient() = RestClient.builder().withLenientStringConverter().build()
}
