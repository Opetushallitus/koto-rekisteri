package fi.oph.kitu.yki.arvioijat

import java.time.LocalDate

object Rekisterikausi {
    const val KAUDEN_PITUUS_VUOSINA = 5L

    fun paattymispaiva(alkupaiva: LocalDate): LocalDate = alkupaiva.plusYears(KAUDEN_PITUUS_VUOSINA)
}
