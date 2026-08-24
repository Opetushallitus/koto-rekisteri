package fi.oph.kitu.yki

import fi.oph.kitu.i18n.UiText

enum class TutkinnonOsa(
    val bitmask: Int,
) {
    PU(1),
    KI(2),
    TY(4),
    PY(8),
    RS(0),
    YL(0),
    ;

    val viewText: String
        get() =
            when (this) {
                PU -> UiText.Yki.Sarake.puhuminen
                KI -> UiText.Yki.Sarake.kirjoittaminen
                TY -> UiText.Yki.Sarake.tekstinYmmartaminen
                PY -> UiText.Yki.Sarake.puheenYmmartaminen
                RS -> UiText.Yki.Sarake.rakenteetJaSanasto
                YL -> UiText.Yki.Sarake.yleisarvosana
            }.toString()

    companion object {
        // Solki-koodit avattuna:
        val puhuminen = PU
        val kirjoittaminen = KI
        val tekstinYmmartaminen = TY
        val puheenYmmartaminen = PY
        val rakenteetJaSanasto = RS
        val yleisarvosana = YL
    }
}
