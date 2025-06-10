package fi.oph.kitu.oppijanumero

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.CookieManager
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.time.Duration

@Configuration
class OppijanumeroHttpClientConfig {
    @Bean("oppijanumeroHttpClient")
    fun HttpClient(): HttpClient =
        HttpClient
            .newBuilder()
            .followRedirects(Redirect.NEVER)
            .cookieHandler(CookieManager()) // sends JSESSIONID Cookie between the requests
            .connectTimeout(Duration.ofSeconds(10))
            .build()
}

@Configuration
class OppijanumeroRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    @Value("\${kitu.oppijanumero.callerid}")
    private lateinit var callerId: String

    @Bean("oppijanumeroRestClient")
    fun oppijanumeroRestClient(
        @Qualifier("oppijanumeroHttpClient")
        httpClient: HttpClient,
    ): RestClient =
        restClientBuilder
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .baseUrl(serviceUrl)
            .defaultHeaders { headers ->
                headers["Caller-Id"] = callerId
                headers["CSRF"] = "CSRF"
                headers["Cookie"] = "CSRF=CSRF"
            }.build()
}
