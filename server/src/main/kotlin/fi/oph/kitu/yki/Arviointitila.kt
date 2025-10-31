package fi.oph.kitu.yki

enum class Arviointitila(
    val viewText: String,
) {
    ARVIOITAVANA("Arvioitavana"),
    EI_SUORITUSTA("Ei suoritusta"),
    KESKEYTETTY("Keskeytetty"),
    ARVIOITU("Arvioitu"),
    UUSINTA("Uusinta"),
}
