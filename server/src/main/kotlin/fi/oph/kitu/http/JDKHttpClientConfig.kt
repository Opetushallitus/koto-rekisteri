package fi.oph.kitu.http

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextStartedEvent
import org.springframework.context.event.GenericApplicationListener
import java.net.CookieHandler
import java.net.CookieManager

/**
 * Configuration class for setting up and customizing [java.net.http.HttpClient].
 *
 * Adds a default cookie handler, which is missing in the default HttpClient configuration.
 */
@Configuration
class JDKHttpClientConfig {
    @Bean
    fun configureCookieManager(): GenericApplicationListener =
        GenericApplicationListener.forEventType(ContextStartedEvent::class.java) { _ ->
            CookieHandler.setDefault(CookieManager())
        }
}
