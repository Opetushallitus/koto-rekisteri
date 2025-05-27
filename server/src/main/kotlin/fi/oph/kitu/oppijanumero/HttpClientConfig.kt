package fi.oph.kitu.oppijanumero

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class HttpClientConfig {
    @Bean("casHttpClient")
    fun httpClient(cookieManager: CookieManager): HttpClient =
        HttpClient
            .newBuilder()
            .cookieHandler(cookieManager) // sends JSESSIONID Cookie between the requests
            .connectTimeout(Duration.ofSeconds(10))
            .build()
}

@Configuration
class CasCookieManagerConfig {
    @Bean
    fun cookieManager(): CookieManager =
        CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
}
