package fi.oph.kitu.i18n

import fi.oph.kitu.koodisto.KoodistoService
import fi.oph.kitu.koodisto.toLocalizedString
import org.springframework.stereotype.Service

@Service
class LocalizationService(
    val koodistoService: KoodistoService,
) {
    fun translationBuilder() = TranslationBuilder(koodistoService)
}

class TranslationBuilder(
    private val koodistoService: KoodistoService,
) {
    private var language: Language = Language.FI
    private var koodistoUris: MutableSet<String> = mutableSetOf()

    fun language(language: Language): TranslationBuilder {
        this.language = language
        return this
    }

    fun koodistot(vararg uri: String): TranslationBuilder {
        this.koodistoUris.addAll(uri)
        return this
    }

    fun build(): Translations =
        try {
            val koodistot =
                koodistoUris.associateWith { uri ->
                    koodistoService
                        .getKoodiviitteet(uri)
                        ?.associate { it.koodiArvo to it.metadata.toLocalizedString() }
                        ?: emptyMap()
                }

            Translations(
                language = language,
                koodistot = koodistot,
            )
        } catch (e: Exception) {
            Translations(language = language, koodistot = emptyMap())
        }
}
