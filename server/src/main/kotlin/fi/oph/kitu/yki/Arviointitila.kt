package fi.oph.kitu.yki

import fi.oph.kitu.html.DisplayEnum
import fi.oph.kitu.html.table.HideInTableFilter
import java.time.LocalDate

// Solki-arviointitilat + Kielitutkintorekisterin omat tilat
enum class Arviointitila(
    val viewText: String,
) : DisplayEnum {
    ILMOITTAUTUNUT("Ilmoittautunut"),
    PERUTTU("Ilmoittautuminen peruttu"),
    EI_SUORITUSTA("Ei suoritusta"),
    ARVIOITAVA("Suoritus arvioitavana"),
    ARVIOITU("Arviointi valmis"),
    TARKISTUSARVIOITAVA("Suoritus tarkistusarvioitavana"),
    TARKISTUSARVIOITU("Tarkistusarviointi tehty"),
    TARKISTUSARVIOINTI_HYVAKSYTTY("Tarkistusarviointi hyväksytty"),

    @Deprecated("Poistuu käytöstä uuden arviointitilamallin myötä")
    @HideInTableFilter
    KESKEYTETTY("Suoritus keskeytetty"),
    ;

    fun arvioitu() = listOf(ARVIOITU, TARKISTUSARVIOITAVA).contains(this) || tarkistusarvioitu()

    fun tarkistusarvioitu() = listOf(TARKISTUSARVIOITU, TARKISTUSARVIOINTI_HYVAKSYTTY).contains(this)

    fun pelkkäIlmoittautuminen() = ilmoittautumistilat.contains(this)

    override fun displayText(): String = viewText

    companion object {
        val ilmoittautumistilat = listOf(ILMOITTAUTUNUT, PERUTTU)
    }
}

fun laskeArviointitila(
    nykyinen: Arviointitila,
    osakoeCount: Int,
    arvosanaPuuttuu: Int,
    oikeitaArvosanoja: Int,
    onTarkistusarviointi: Boolean,
    tarkistuksenKasittelypaiva: LocalDate?,
): Arviointitila =
    when (nykyinen) {
        Arviointitila.ILMOITTAUTUNUT,
        Arviointitila.PERUTTU,
        Arviointitila.TARKISTUSARVIOINTI_HYVAKSYTTY,
        -> {
            nykyinen
        }

        else -> {
            when {
                onTarkistusarviointi && tarkistuksenKasittelypaiva != null -> Arviointitila.TARKISTUSARVIOITU
                onTarkistusarviointi -> Arviointitila.TARKISTUSARVIOITAVA
                osakoeCount == 0 -> nykyinen
                arvosanaPuuttuu > 0 -> Arviointitila.ARVIOITAVA
                oikeitaArvosanoja == 0 -> Arviointitila.EI_SUORITUSTA
                else -> Arviointitila.ARVIOITU
            }
        }
    }
