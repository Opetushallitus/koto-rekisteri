package fi.oph.kitu.yki

enum class TutkinnonOsa(
    val bitmask: Int,
    val viewText: String,
) {
    PU(1, "Puhuminen"),
    KI(2, "Kirjoittaminen"),
    TY(4, "Tekstin ymmärtäminen"),
    PY(8, "Puheen ymmärtäminen"),
    RS(0, "Rakenteet ja sanasto"),
    YL(0, "Yleisarvosana"),
    ;

    companion object {
        // Solki-koodit avattuna:
        val puhuminen = PU
        val kirjoittaminen = KI
        val tekstinYmmartaminen = TY
        val puheenYmmartaminen = PY
        val rakenteetJaSanasto = RS
        val yleisarvosana = YL

        fun fromBits(bits: Int): Set<TutkinnonOsa> = entries.filter { bits and it.bitmask > 0 }.toSet()

        fun Int.toTutkinnonOsaSet(): Set<TutkinnonOsa> = fromBits(this)
    }
}
