package fi.oph.kitu.koodisto

import fi.oph.kitu.i18n.LocalizedString

data class KoodistopalveluKoodiviite(
    val koodiUri: String,
    val koodiArvo: String,
    val versio: Int,
    val metadata: List<KoodistopalveluKoodiviiteMetadata>,
)

data class KoodistopalveluKoodiviiteMetadata(
    val nimi: String,
    val kieli: KoodistopalveluLanguage,
)

enum class KoodistopalveluLanguage {
    FI,
    SV,
    EN,
}

fun List<KoodistopalveluKoodiviiteMetadata>.toLocalizedString(): LocalizedString =
    fold(LocalizedString()) { acc, metadata ->
        when (metadata.kieli) {
            KoodistopalveluLanguage.FI -> acc.copy(fi = metadata.nimi)
            KoodistopalveluLanguage.SV -> acc.copy(sv = metadata.nimi)
            KoodistopalveluLanguage.EN -> acc.copy(en = metadata.nimi)
        }
    }
