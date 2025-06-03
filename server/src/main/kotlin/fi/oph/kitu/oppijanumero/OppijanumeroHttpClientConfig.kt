package fi.oph.kitu.oppijanumero

import io.opentelemetry.instrumentation.javahttpclient.JavaHttpClientTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.CookieManager
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class OppijanumeroHttpClientConfig {
    @Bean("oppijanumeroHttpClient")
    fun HttpClient(openTelemetry: OpenTelemetrySdk): HttpClient {
        val httpClient =
            HttpClient
                .newBuilder()
                .cookieHandler(CookieManager()) // sends JSESSIONID Cookie between the requests
                .connectTimeout(Duration.ofSeconds(10))
                .build()

        return JavaHttpClientTelemetry
            .builder(openTelemetry)
            .build()
            .newHttpClient(httpClient)
    }
}

@Configuration
class OppijanumeroRestClientConfig(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    @Bean("oppijanumeroRestClient")
    fun oppijanumeroRestClient(
        @Qualifier("oppijanumeroHttpClient")
        httpClient: HttpClient,
    ): RestClient =
        restClientBuilder
            .requestFactory(JdkClientHttpRequestFactory(httpClient))
            .baseUrl(serviceUrl)
            .build()
}
