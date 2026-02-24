package fi.oph.kitu.yki

// Solki-arviointitilat + Kielitutkintorekisterin omat tilat
enum class Arviointitila(
    val viewText: String,
) {
    ARVIOITAVA("Suoritus arvioitavana"),
    ARVIOITU("Arviointi valmis"),

    EI_SUORITUSTA("Ei suoritusta"),
    KESKEYTETTY("Suoritus keskeytetty"),

    TARKISTUSARVIOITAVA("Suoritus tarkistusarvioitavana"),
    TARKISTUSARVIOITU("Tarkistusarviointi tehty"),
    TARKISTUSARVIOINTI_HYVAKSYTTY("Tarkistusarviointi hyväksytty"),
    ;

    fun arvioitu() = listOf(ARVIOITU, TARKISTUSARVIOITAVA).contains(this) || tarkistusarvioitu()

    fun tarkistusarvioitu() = listOf(TARKISTUSARVIOITU, TARKISTUSARVIOINTI_HYVAKSYTTY).contains(this)
}
