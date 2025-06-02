package fi.oph.kitu.oppijanumero

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.http.HttpClient
import java.time.Duration
import kotlin.text.set

/**
 * REST client configuration for CAS.
 *
 * - Uses `casRestClientBuilder`: `RestClient.Builder`
 * - Uses `casHttpClient`: `HttpClient`
 * - Uses `CookieManager`
 */
@Configuration
class RestClientConfig(
    @Qualifier("casRestClientBuilder")
    private val restClientBuilder: RestClient.Builder,
) {
    @Bean("casRestClient")
    fun casRestClient(): RestClient = restClientBuilder.build()
}

@Configuration
class CasRestClientBuilderConfig {
    // The code assumes this value has trailing slash
    @Value("\${kitu.oppijanumero.url}")
    private lateinit var baseUrl: String

    @Value("\${kitu.oppijanumero.callerid}")
    private lateinit var callerId: String

    @Bean("casRestClientBuilder")
    fun casRestClientBuilder(): RestClient.Builder {
        val cookieManager =
            CookieManager().apply {
                setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            }

        val httpClient =
            HttpClient
                .newBuilder()
                .cookieHandler(cookieManager) // sends JSESSIONID Cookie between the requests
                .connectTimeout(Duration.ofSeconds(10))
                .build()

        val requestFactory = JdkClientHttpRequestFactory(httpClient)

        val builder =
            RestClient
                .builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeaders { headers ->
                    headers["Caller-Id"] = callerId
                    headers["CSRF"] = "CSRF"
                    headers["Cookie"] = "CSRF=CSRF"
                }

        return builder
    }
}
