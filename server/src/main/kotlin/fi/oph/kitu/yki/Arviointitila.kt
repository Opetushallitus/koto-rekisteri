package fi.oph.kitu.yki

// Arviointitilat, joita Solki lähettää Kielitutkintorekisteriin
enum class SolkiArviointitila {
    ARVIOITAVA,
    EI_SUORITUSTA,
    KESKEYTETTY,
    ARVIOITU,
    UUSITTAVA,
    TARKISTUSARVIOITAVA,
    TARKISTUSARVIOITU,
    ;

    fun toKituArviointitila(): KituArviointitila = KituArviointitila.valueOf(name)
}

// Solki-arviointitilat + Kielitutkintorekisterin omat tilat
enum class KituArviointitila(
    val viewText: String,
) {
    ARVIOITAVA("Suoritus arvioitavana"),
    ARVIOITU("Arviointi valmis"),

    EI_SUORITUSTA("Ei suoritusta"),
    KESKEYTETTY("Suoritus keskeytetty"),
    UUSITTAVA("Uusittava teknisen virheen vuoksi"),

    TARKISTUSARVIOITAVA("Suoritus tarkistusarvioitavana"),
    TARKISTUSARVIOITU("Tarkistusarviointi tehty"),
    TARKISTUSARVIOINTI_HYVAKSYTTY("Tarkitusarviointi hyväksytty"),
    ;

    fun arvioitu() = this == ARVIOITU || tarkistusarvioitu()

    fun tarkistusarvioitu() = listOf(TARKISTUSARVIOITU, TARKISTUSARVIOINTI_HYVAKSYTTY).contains(this)
}
