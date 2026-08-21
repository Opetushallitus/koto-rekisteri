package fi.oph.kitu.yki

import fi.oph.kitu.html.table.Nimetty
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText

enum class Tutkintotaso : Nimetty {
    /** Perustaso*/
    PT,

    /** Keskitaso*/
    KT,

    /** Ylin taso*/
    YT,
    ;

    override val nimi: LocalizedString
        get() =
            when (this) {
                PT -> UiText.Yki.Taso.perustaso
                KT -> UiText.Yki.Taso.keskitaso
                YT -> UiText.Yki.Taso.ylinTaso
            }
}
