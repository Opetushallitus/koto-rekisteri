package fi.oph.kitu.yki.arvioijat

import fi.oph.kitu.html.table.Nimetty
import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.i18n.UiText

enum class YkiArvioijaTila : Nimetty {
    AKTIIVINEN,
    PASSIVOITU,
    ;

    override val nimi: LocalizedString
        get() =
            when (this) {
                AKTIIVINEN -> UiText.Yki.ArvioijaTila.aktiivinen
                PASSIVOITU -> UiText.Yki.ArvioijaTila.passivoitu
            }
}
