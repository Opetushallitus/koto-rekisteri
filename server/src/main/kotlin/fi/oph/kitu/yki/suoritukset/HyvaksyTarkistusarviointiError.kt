package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.i18n.finnishDate
import java.time.LocalDate

sealed interface HyvaksyTarkistusarviointiError {
    val message: String

    data class EiTarkistusarvioitu(
        val suorituksenNimi: String,
    ) : HyvaksyTarkistusarviointiError {
        override val message: String
            get() = "Tarkistusarvioimatonta suoritusta $suorituksenNimi ei voi asettaa hyväksytyksi"
    }

    data class KasittelyPvmPuuttuu(
        val suorituksenNimi: String,
    ) : HyvaksyTarkistusarviointiError {
        override val message: String
            get() = "Tarkistusarviointia suoritukselle $suorituksenNimi ei voi hyväksyä, ennen kuin se on käsitelty."
    }

    data class PaivamaaraEnnenKasittelya(
        val suorituksenNimi: String,
        val pvm: LocalDate,
        val kasittelyPvm: LocalDate,
    ) : HyvaksyTarkistusarviointiError {
        override val message: String
            get() =
                "Tarkistusarviointi suoritukselle $suorituksenNimi ei voi hyväksyä päivämäärällä " +
                    "${pvm.finnishDate()}, koska se on aiemmin kuin käsittelypäivä ${kasittelyPvm.finnishDate()}."
    }
}
