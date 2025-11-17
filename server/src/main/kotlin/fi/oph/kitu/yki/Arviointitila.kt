package fi.oph.kitu.yki

// Arviointitilat, joita Solki lähettää Kielitutkintorekisteriin
enum class SolkiArviointitila {
    ARVIOITAVANA,
    EI_SUORITUSTA,
    KESKEYTETTY,
    ARVIOITU,
    UUSITTAVA,
    TARKISTUSARVIOITU,
    ;

    fun toKituArviointitila(): KituArviointitila = KituArviointitila.valueOf(name)
}

// Solki-arviointitilat + Kielitutkintorekisterin omat tilat
enum class KituArviointitila(
    val viewText: String,
) {
    ARVIOITAVANA("Suoritus arvioitavana"),
    EI_SUORITUSTA("Ei suoritusta"),
    KESKEYTETTY("Suoritus keskeytetty"),
    ARVIOITU("Arviointi valmis"),
    UUSITTAVA("Uusittava teknisen virheen vuoksi"),
    TARKISTUSARVIOITU("Tarkistusarviointi tehty"),
    TARKISTUSARVIOINTI_HYVAKSYTTY("Tarkitusarviointi hyväksytty"),
    ;

    fun arviointiValmis() = this == ARVIOITU || this == TARKISTUSARVIOINTI_HYVAKSYTTY

    fun tarkistusarvioitu() = this == TARKISTUSARVIOITU || this == TARKISTUSARVIOINTI_HYVAKSYTTY
}
