package fi.oph.kitu.webmvc

import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritusColumn
import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritusErrorColumn
import fi.oph.kitu.vkt.html.VktIlmoittautuneet
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorColumn
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.apply {
            addEnumConverter<YkiSuoritusColumn>(YkiSuoritusColumn::urlParam)
            addEnumConverter<YkiSuoritusErrorColumn>(YkiSuoritusErrorColumn::urlParam)
            addEnumConverter<YkiArvioijaColumn>(YkiArvioijaColumn::urlParam)
            addEnumConverter<KielitestiSuoritusColumn>(KielitestiSuoritusColumn::urlParam)
            addEnumConverter<KielitestiSuoritusErrorColumn>(KielitestiSuoritusErrorColumn::urlParam)
            addEnumConverter<VktIlmoittautuneet.Column>(VktIlmoittautuneet.Column::urlParam)
        }
    }

    private final inline fun <reified E : Enum<E>> FormatterRegistry.addEnumConverter(
        crossinline urlParamFieldGetter: (E) -> String,
        ignoreCase: Boolean = true,
    ) {
        this.addConverter(String::class.java, E::class.java) { source ->
            enumValues<E>().find {
                source.equals(urlParamFieldGetter(it), ignoreCase)
            }
        }
    }
}
