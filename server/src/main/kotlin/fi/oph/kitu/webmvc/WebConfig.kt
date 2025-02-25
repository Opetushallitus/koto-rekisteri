package fi.oph.kitu.webmvc

import fi.oph.kitu.yki.suoritukset.StringToYkiSuoritusColumnConverter
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig {
    @Configuration
    class WebConfig : WebMvcConfigurer {
        override fun addFormatters(registry: FormatterRegistry) {
            registry.addConverter(StringToYkiSuoritusColumnConverter())
        }
    }
}
