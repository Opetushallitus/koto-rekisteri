package fi.oph.kitu.i18n

import fi.oph.kitu.koodisto.Koodisto

typealias KoodistoTranslations = Map<String, KoodiTranslations>
typealias KoodiTranslations = Map<String, LocalizedString>

data class Translations(
    val language: Language,
    val koodistot: KoodistoTranslations,
) {
    fun get(
        a: Koodisto.Koodiviite?,
        default: String = "",
    ): String = a?.let { get(it) } ?: default

    fun get(a: Koodisto.Koodiviite): String = getByKoodiviite(a.koodistoUri, a.koodiarvo)

    fun getByKoodiviite(
        koodistoUri: String,
        koodiarvo: String?,
        default: String = "",
    ): String = koodiarvo?.let { getByKoodiviite(koodistoUri, it) } ?: default

    fun getByKoodiviite(
        koodistoUri: String,
        koodiarvo: String,
    ): String =
        koodistot[koodistoUri]
            ?.get(koodiarvo)
            ?.get(language)
            ?: "<$koodistoUri:$koodiarvo>"
}
