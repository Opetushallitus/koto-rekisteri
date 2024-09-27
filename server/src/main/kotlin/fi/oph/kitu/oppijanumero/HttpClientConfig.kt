package fi.oph.kitu.oppijanumero

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.CookieManager
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class HttpClientConfig {
    @Bean("oppijanumeroHttpClient")
    fun HttpClient(): HttpClient =
        HttpClient
            .newBuilder()
            .cookieHandler(CookieManager()) // sends JSESSIONID Cookie between the requests
            .connectTimeout(Duration.ofSeconds(10))
            .build()
}
