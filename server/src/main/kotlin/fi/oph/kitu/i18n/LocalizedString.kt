package fi.oph.kitu.i18n

enum class Language {
    FI,
    SV,
    EN,
}

data class LocalizedString(
    val fi: String? = null,
    val sv: String? = null,
    val en: String? = null,
) {
    override fun toString(): String = fi ?: sv ?: en ?: "<invalid LocalizedString>"

    fun get(lang: Language): String? =
        when (lang) {
            Language.FI -> fi
            Language.SV -> sv
            Language.EN -> en
        }
}
