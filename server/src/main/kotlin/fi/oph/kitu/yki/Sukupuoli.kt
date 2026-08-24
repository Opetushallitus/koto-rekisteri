package fi.oph.kitu.yki

import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText

enum class Sukupuoli(
    val text: LocalizedString,
) {
    M(UiText.Sukupuoli.mies),
    N(UiText.Sukupuoli.nainen),
    E(UiText.Sukupuoli.eiTiedossa),
}
