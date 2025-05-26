package fi.oph.kitu.oppijanumero

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.CookieManager
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class CasRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.oppijanumero.casUrl}")
    private lateinit var baseUrl: String

    @Value("\${kitu.oppijanumero.callerid}")
    private lateinit var callerId: String

    fun casHttpClient(): HttpClient =
        HttpClient
            .newBuilder()
            .cookieHandler(CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    @Bean("casRestClient")
    fun casRestClient(): RestClient =
        restClientBuilder
            .requestFactory(JdkClientHttpRequestFactory(casHttpClient()))
            .baseUrl(baseUrl)
            .defaultHeaders { headers ->
                headers["Caller-Id"] = callerId
                headers["CSRF"] = "CSRF"
                headers["Cookie"] = "CSRF=CSRF"
            }.build()
}
