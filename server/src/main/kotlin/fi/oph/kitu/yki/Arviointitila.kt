package fi.oph.kitu.yki

// Solki-arviointitilat + Kielitutkintorekisterin omat tilat
enum class Arviointitila(
    val viewText: String,
) {
    ILMOITTAUTUNUT("Ilmoittautunut"),
    PERUTTU("Ilmoittautuminen peruttu"),
    EI_SUORITUSTA("Ei suoritusta"),
    ARVIOITAVA("Suoritus arvioitavana"),
    ARVIOITU("Arviointi valmis"),
    TARKISTUSARVIOITAVA("Suoritus tarkistusarvioitavana"),
    TARKISTUSARVIOITU("Tarkistusarviointi tehty"),
    TARKISTUSARVIOINTI_HYVAKSYTTY("Tarkistusarviointi hyväksytty"),

    @Deprecated("Poistuu käytöstä uuden arviointitilamallin myötä")
    KESKEYTETTY("Suoritus keskeytetty"),
    ;

    fun arvioitu() = listOf(ARVIOITU, TARKISTUSARVIOITAVA).contains(this) || tarkistusarvioitu()

    fun tarkistusarvioitu() = listOf(TARKISTUSARVIOITU, TARKISTUSARVIOINTI_HYVAKSYTTY).contains(this)
}
