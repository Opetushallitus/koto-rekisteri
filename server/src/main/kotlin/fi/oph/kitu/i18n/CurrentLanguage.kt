package fi.oph.kitu.i18n

import org.springframework.context.i18n.LocaleContextHolder
import java.util.Locale

object CurrentLanguage {
    fun get(): Language {
        val locale = LocaleContextHolder.getLocaleContext()?.locale ?: return Language.FI
        return fromLocale(locale)
    }

    fun fromLocale(locale: Locale): Language =
        when (locale.language) {
            "sv" -> Language.SV
            "en" -> Language.EN
            else -> Language.FI
        }
}
