package fi.oph.kitu.oppijanumero

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
class RestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.oppijanumero.url}")
    private lateinit var baseUrl: String

    @Value("\${kitu.oppijanumero.callerid}")
    private lateinit var callerId: String

    @Bean("casRestClient")
    fun casRestClient(
        @Qualifier("casHttpClient")
        httpClient: HttpClient,
    ): RestClient =
        restClientBuilder
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .baseUrl(baseUrl)
            .defaultHeaders { headers ->
                headers["Caller-Id"] = callerId
                headers["CSRF"] = "CSRF"
                headers["Cookie"] = "CSRF=CSRF"
            }.build()
}
