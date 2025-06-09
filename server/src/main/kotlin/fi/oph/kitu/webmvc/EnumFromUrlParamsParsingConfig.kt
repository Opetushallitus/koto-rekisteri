package fi.oph.kitu.webmvc

import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritusColumn
import fi.oph.kitu.kotoutumiskoulutus.KielitestiSuoritusErrorColumn
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.yki.arvioijat.YkiArvioijaColumn
import fi.oph.kitu.yki.suoritukset.YkiSuoritusColumn
import fi.oph.kitu.yki.suoritukset.error.YkiSuoritusErrorColumn
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class EnumFromUrlParamsParsingConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.apply {
            addEnumFromUrlParamParser<YkiSuoritusColumn>(YkiSuoritusColumn::urlParam)
            addEnumFromUrlParamParser<YkiSuoritusErrorColumn>(YkiSuoritusErrorColumn::urlParam)
            addEnumFromUrlParamParser<YkiArvioijaColumn>(YkiArvioijaColumn::urlParam)
            addEnumFromUrlParamParser<KielitestiSuoritusColumn>(KielitestiSuoritusColumn::urlParam)
            addEnumFromUrlParamParser<KielitestiSuoritusErrorColumn>(KielitestiSuoritusErrorColumn::urlParam)
            addEnumFromUrlParamParser<CustomVktSuoritusRepository.Column>(CustomVktSuoritusRepository.Column::urlParam)
        }
    }

    private final inline fun <reified E : Enum<E>> FormatterRegistry.addEnumFromUrlParamParser(
        crossinline urlParamFieldGetter: (E) -> String,
    ) {
        this.addConverter(String::class.java, E::class.java) { source ->
            enumValues<E>().find {
                source.equals(urlParamFieldGetter(it), ignoreCase = true)
            }
        }
    }
}
