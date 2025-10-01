package fi.oph.kitu.yki

enum class TutkinnonOsa {
    PU,
    KI,
    TY,
    PY,
    RS,
    YL,
    ;

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
