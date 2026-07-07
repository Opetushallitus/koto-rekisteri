package fi.oph.kitu.i18n

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import java.util.Locale

@Configuration
class LocalizationConfig : WebMvcConfigurer {
    @Bean
    fun localeResolver(): LocaleResolver =
        CookieLocaleResolver("kitu_lang").apply {
            setDefaultLocale(Locale.of("fi"))
            setCookieHttpOnly(true)
            setCookieSameSite("Lax")
        }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(
            LocaleChangeInterceptor().apply {
                paramName = "lang"
                isIgnoreInvalidLocale = true
            },
        )
    }
}
