package fi.oph.kitu.i18n

import fi.oph.kitu.koodisto.KoodistoService
import fi.oph.kitu.koodisto.toLocalizedString
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Tracer
import org.springframework.stereotype.Service

@Service
class LocalizationService(
    private val koodistoService: KoodistoService,
    private val tracer: Tracer,
) {
    fun translationBuilder() = TranslationBuilder(koodistoService, tracer)
}

class TranslationBuilder(
    private val koodistoService: KoodistoService,
    private val tracer: Tracer,
) {
    private var language: Language = Language.FI
    private var koodistoUris: MutableSet<String> = mutableSetOf()

    fun language(language: Language): TranslationBuilder =
        tracer
            .spanBuilder("language")
            .startSpan()
            .use { span ->
                span.setAttribute("language", language.name)
                this.language = language
                return this
            }

    fun koodistot(vararg uri: String): TranslationBuilder =
        tracer
            .spanBuilder("koodistot")
            .startSpan()
            .use { span ->
                span.setAttribute("koodistoUris", koodistoUris.joinToString(" "))
                this.koodistoUris.addAll(uri)
                return this
            }

    fun build(): Translations =
        tracer
            .spanBuilder("build")
            .startSpan()
            .use { span ->
                try {
                    val koodistot =
                        koodistoUris.associateWith { uri ->
                            koodistoService
                                .getKoodiviitteet(uri)
                                ?.associate { it.koodiArvo to it.metadata.toLocalizedString() }
                                .orEmpty()
                        }

                    Translations(
                        language = language,
                        koodistot = koodistot,
                    )
                } catch (e: Exception) {
                    Translations(language = language, koodistot = emptyMap())
                }
            }
}
