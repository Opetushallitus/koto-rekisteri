package fi.oph.kitu.kotoutumiskoulutus.suoritukset

enum class Testikieli {
    FIN,
    SWE,
    ;

    companion object {
        fun fromString(str: String): Testikieli =
            when (str) {
                "fi" -> FIN
                "sv" -> SWE
                else -> throw IllegalArgumentException("Invalid language code $str")
            }
    }
}
