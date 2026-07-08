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

    fun get(lang: Language): String =
        when (lang) {
            Language.FI -> fi
            Language.SV -> sv
            Language.EN -> en
        } ?: toString()

    fun contains(
        other: CharSequence,
        ignoreCase: Boolean = false,
    ): Boolean = listOfNotNull(fi, sv, en).any { it.contains(other, ignoreCase) }

    fun interpolate(vararg args: Pair<String, Any?>): LocalizedString {
        fun substitute(text: String?): String? =
            args.fold(text) { acc, (name, value) -> acc?.replace("{$name}", value.toString()) }
        return LocalizedString(fi = substitute(fi), sv = substitute(sv), en = substitute(en))
    }
}
