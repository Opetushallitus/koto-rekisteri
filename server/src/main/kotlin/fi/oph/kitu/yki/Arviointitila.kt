package fi.oph.kitu.yki

// Arviointitilat, joita Solki lähettää Kielitutkintorekisteriin
enum class SolkiArviointitila {
    ARVIOITAVANA,
    EI_SUORITUSTA,
    KESKEYTETTY,
    ARVIOITU,
    UUSINTA,
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
    UUSINTA("Uusinta teknisen virheen vuoksi"),
    TARKISTUSARVIOITU("Tarkistusarviointi tehty"),
    TARKITUSARVIOINTI_HYVAKSYTTY("Tarkitusarviointi hyväksytty"),
}
