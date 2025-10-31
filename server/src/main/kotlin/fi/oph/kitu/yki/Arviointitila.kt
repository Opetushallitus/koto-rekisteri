package fi.oph.kitu.yki

enum class Arviointitila(
    val viewText: String,
) {
    ARVIOITAVANA("Suoritus arvioitavana"),
    EI_SUORITUSTA("Ei suoritusta"),
    KESKEYTETTY("Suoritus keskeytetty"),
    ARVIOITU("Arviointi valmis"),
    UUSINTA("Uusinta teknisen virheen vuoksi"),
    TARKISTUSARVIOITU("Tarkistusarviointi tehty"),
}
