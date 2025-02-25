package fi.oph.kitu.webmvc

import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.apply {
            addEnumConverter<YkiSuoritusColumn>()
            addEnumConverter<YkiArvioijaColumn>()
        }
    }

    final inline fun <reified E : Enum<E>> FormatterRegistry.addEnumConverter(ignoreCase: Boolean = true) {
        this.addConverter(String::class.java, E::class.java) { source ->
            enumValues<E>().find {
                it.name.equals(source, ignoreCase)
            }
        }
    }
}
