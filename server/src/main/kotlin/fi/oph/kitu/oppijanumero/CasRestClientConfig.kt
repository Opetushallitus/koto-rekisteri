package fi.oph.kitu.oppijanumero

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
class CasRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.oppijanumero.casUrl}")
    private lateinit var casUrl: String

    @Bean("casRestClient")
    fun casRestClient(
        @Qualifier("oppijanumeroHttpClient")
        httpClient: HttpClient,
    ): RestClient =
        restClientBuilder
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .baseUrl(casUrl)
            .build()
}
